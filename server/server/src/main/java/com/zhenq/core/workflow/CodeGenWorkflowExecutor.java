package com.zhenq.core.workflow;

import cn.hutool.core.util.StrUtil;
import com.zhenq.ai.AiCodeGeneratorService;
import com.zhenq.ai.model.MultiFileCodeResult;
import com.zhenq.config.AppProperties;
import com.zhenq.constant.AppConstant;
import com.zhenq.core.parser.CodeParser;
import com.zhenq.core.quality.CodeQualityChecker;
import com.zhenq.core.quality.CodeQualityReport;
import com.zhenq.core.saver.CodeFileSaverExecutor;
import com.zhenq.core.workflow.edge.QualityCheckRouterEdge;
import com.zhenq.core.workflow.node.CollectAssetsNode;
import com.zhenq.core.workflow.node.EnrichPromptNode;
import com.zhenq.core.workflow.node.QualityCheckNode;
import com.zhenq.core.workflow.node.RetryGenerateNode;
import com.zhenq.core.workflow.node.SaveCodeNode;
import com.zhenq.model.enums.CodeGenTypeEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.RedisSaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraph4j 代码生成工作流执行器
 * <p>
 * 准备阶段：collect_assets -> enrich_prompt<br>
 * 收尾阶段：quality_check -[条件边]-> retry_generate | save_code（支持 Redis Checkpoint）
 */
@Slf4j
@Service
public class CodeGenWorkflowExecutor {

    @Resource
    private CollectAssetsNode collectAssetsNode;

    @Resource
    private EnrichPromptNode enrichPromptNode;

    @Resource
    private QualityCheckNode qualityCheckNode;

    @Resource
    private RetryGenerateNode retryGenerateNode;

    @Resource
    private SaveCodeNode saveCodeNode;

    @Resource
    private QualityCheckRouterEdge qualityCheckRouterEdge;

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private CodeQualityChecker codeQualityChecker;

    @Resource
    private AppProperties appProperties;

    @Autowired(required = false)
    private RedisSaver codeGenWorkflowRedisSaver;

    private CompiledGraph<CodeGenWorkflowState> prepareGraph;
    private CompiledGraph<CodeGenWorkflowState> finalizeGraph;

    @PostConstruct
    void initGraph() throws GraphStateException {
        CompileConfig prepareConfig = CompileConfig.builder().build();
        CompileConfig finalizeConfig = buildCheckpointCompileConfig();
        prepareGraph = new StateGraph<>(CodeGenWorkflowState.SCHEMA, CodeGenWorkflowState::new)
                .addNode("collect_assets", node_async(collectAssetsNode))
                .addNode("enrich_prompt", node_async(enrichPromptNode))
                .addEdge(START, "collect_assets")
                .addEdge("collect_assets", "enrich_prompt")
                .addEdge("enrich_prompt", END)
                .compile(prepareConfig);

        finalizeGraph = new StateGraph<>(CodeGenWorkflowState.SCHEMA, CodeGenWorkflowState::new)
                .addNode("quality_check", node_async(qualityCheckNode))
                .addNode("retry_generate", node_async(retryGenerateNode))
                .addNode("save_code", node_async(saveCodeNode))
                .addEdge(START, "quality_check")
                .addConditionalEdges(
                        "quality_check",
                        qualityCheckRouterEdge,
                        Map.of(
                                QualityCheckRouterEdge.SAVE_CODE, "save_code",
                                QualityCheckRouterEdge.RETRY_GENERATE, "retry_generate"
                        ))
                .addEdge("retry_generate", "quality_check")
                .addEdge("save_code", END)
                .compile(finalizeConfig);
    }

    private CompileConfig buildCheckpointCompileConfig() {
        if (appProperties.isWorkflowCheckpointEnabled() && codeGenWorkflowRedisSaver != null) {
            return CompileConfig.builder()
                    .checkpointSaver(codeGenWorkflowRedisSaver)
                    .build();
        }
        return CompileConfig.builder().build();
    }

    private RunnableConfig runnableConfig(Long appId, String phase) {
        RunnableConfig.Builder builder = RunnableConfig.builder()
                .threadId("codegen-app-" + appId + "-" + phase);
        return builder.build();
    }

    /**
     * 工作流准备：搜集素材并增强 Prompt
     */
    public String preparePrompt(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        Map<String, Object> input = baseInput(userMessage, codeGenType, appId);
        try {
            CodeGenWorkflowState state = prepareGraph.invoke(input, runnableConfig(appId, "prepare"))
                    .orElseThrow(() -> new IllegalStateException("工作流准备阶段未返回状态"));
            logWorkflow(state);
            String enriched = state.enrichedPrompt();
            return StrUtil.isNotBlank(enriched) ? enriched : userMessage;
        } catch (Exception e) {
            log.warn("工作流准备阶段失败，回退原始 Prompt", e);
            return userMessage;
        }
    }

    /**
     * 同步生成：工作流内直接调用 AI 并落盘（含质量重试）
     */
    public File generateAndSave(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        String enrichedPrompt = preparePrompt(userMessage, codeGenType, appId);
        String generatedCode = switch (codeGenType) {
            case HTML -> aiCodeGeneratorService.generateHtmlCode(enrichedPrompt).getHtmlCode();
            case MULTI_FILE -> mergeMultiFileMarkdown(aiCodeGeneratorService.generateMultiFileCode(enrichedPrompt));
            case VUE_PROJECT -> throw new IllegalArgumentException("Vue 工程请走专用 Agent 流程");
        };
        return finalizeGeneration(generatedCode, codeGenType, appId, enrichedPrompt, userMessage);
    }

    /**
     * 流式/同步生成完成后：质量检测、条件重试、落盘
     */
    public File finalizeGeneration(String generatedCode, CodeGenTypeEnum codeGenType, Long appId) {
        return finalizeGeneration(generatedCode, codeGenType, appId, null, null);
    }

    private File finalizeGeneration(String generatedCode,
                                    CodeGenTypeEnum codeGenType,
                                    Long appId,
                                    String enrichedPrompt,
                                    String userMessage) {
        Map<String, Object> input = new HashMap<>();
        input.put(CodeGenWorkflowState.GENERATED_CODE, generatedCode);
        input.put(CodeGenWorkflowState.CODE_GEN_TYPE, codeGenType.getValue());
        input.put(CodeGenWorkflowState.APP_ID, appId);
        input.put(CodeGenWorkflowState.RETRY_COUNT, 0);
        if (StrUtil.isNotBlank(enrichedPrompt)) {
            input.put(CodeGenWorkflowState.ENRICHED_PROMPT, enrichedPrompt);
        }
        if (StrUtil.isNotBlank(userMessage)) {
            input.put(CodeGenWorkflowState.USER_MESSAGE, userMessage);
        }
        try {
            CodeGenWorkflowState state = finalizeGraph.invoke(input, runnableConfig(appId, "finalize"))
                    .orElseThrow(() -> new IllegalStateException("工作流收尾阶段未返回状态"));
            logWorkflow(state);
            CodeQualityReport report = state.qualityReport();
            if (report != null && !report.isPassed()) {
                log.warn("代码质量未完全通过，已落盘 appId={}: {}", appId, report.summary());
            }
            return getCodeOutputDir(codeGenType, appId);
        } catch (Exception e) {
            log.warn("工作流收尾阶段异常，直接落盘: {}", e.getMessage());
            parseAndSave(generatedCode, codeGenType, appId);
            return getCodeOutputDir(codeGenType, appId);
        }
    }

    /**
     * Vue 工程构建完成后的质量检测
     */
    public CodeQualityReport finalizeVueProject(Long appId) {
        CodeQualityReport report = codeQualityChecker.checkVueProject(appId);
        log.info("[workflow] vue_quality_check -> appId={}, {}", appId, report.summary());
        return report;
    }

    private File parseAndSave(String generatedCode, CodeGenTypeEnum codeGenType, Long appId) {
        switch (codeGenType) {
            case HTML -> CodeFileSaverExecutor.executeSaver(
                    CodeParser.parseHtmlCode(generatedCode), CodeGenTypeEnum.HTML, appId);
            case MULTI_FILE -> CodeFileSaverExecutor.executeSaver(
                    CodeParser.parseMultiFileCode(generatedCode), CodeGenTypeEnum.MULTI_FILE, appId);
            default -> throw new IllegalArgumentException("Vue 工程请走专用 Agent 流程");
        }
        return getCodeOutputDir(codeGenType, appId);
    }

    private File getCodeOutputDir(CodeGenTypeEnum codeGenType, Long appId) {
        return new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                + codeGenType.getValue() + "_" + appId);
    }

    private void logWorkflow(CodeGenWorkflowState state) {
        for (String line : state.workflowLog()) {
            log.info("[workflow-log] {}", line);
        }
    }

    private Map<String, Object> baseInput(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        Map<String, Object> input = new HashMap<>();
        input.put(CodeGenWorkflowState.USER_MESSAGE, userMessage);
        input.put(CodeGenWorkflowState.CODE_GEN_TYPE, codeGenType.getValue());
        input.put(CodeGenWorkflowState.APP_ID, appId);
        return input;
    }

    private String mergeMultiFileMarkdown(MultiFileCodeResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.getHtmlCode() != null) {
            sb.append("```html\n").append(result.getHtmlCode()).append("\n```\n");
        }
        if (result.getCssCode() != null) {
            sb.append("```css\n").append(result.getCssCode()).append("\n```\n");
        }
        if (result.getJsCode() != null) {
            sb.append("```javascript\n").append(result.getJsCode()).append("\n```\n");
        }
        return sb.toString();
    }
}
