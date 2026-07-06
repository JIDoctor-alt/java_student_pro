package com.zhenq.core.vue;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhenq.ai.AiModelScenario;
import com.zhenq.ai.VueProjectAgentFactory;
import com.zhenq.core.quality.CodeQualityReport;
import com.zhenq.core.workflow.CodeGenWorkflowExecutor;
import com.zhenq.model.enums.CodeGenTypeEnum;
import com.zhenq.monitor.AiMetricsCollector;
import com.zhenq.service.AiModelRuntimeResolver;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Vue 工程代码生成执行器（Agent 流式 + 工具调用 + 构建）
 */
@Slf4j
@Service
public class VueProjectCodegenExecutor {

    @Resource
    private VueProjectAgentFactory vueProjectAgentFactory;

    @Resource
    private VueProjectBuildService vueProjectBuildService;

    @Resource
    private VueProjectFileTool vueProjectFileTool;

    @Resource
    private CodeGenWorkflowExecutor codeGenWorkflowExecutor;

    @Resource
    private AiMetricsCollector aiMetricsCollector;

    @Resource
    private AiModelRuntimeResolver aiModelRuntimeResolver;

    /**
     * 启动 Agent 流式生成（异步，通过 callback 推送事件）
     */
    public void executeStream(Long appId, String prompt, VueProjectStreamCallback callback) {
        vueProjectFileTool.bindAppId(appId);
        vueProjectFileTool.bindStreamListener(callback::onToolFile);
        String enrichedPrompt = codeGenWorkflowExecutor.preparePrompt(
                prompt, CodeGenTypeEnum.VUE_PROJECT, appId);
        StringBuilder fullText = new StringBuilder();
        try {
            vueProjectAgentFactory.getAgent().generateVueProjectStream(enrichedPrompt)
                    .onPartialResponse(partial -> {
                        if (shouldSkipPartial(partial)) {
                            return;
                        }
                        fullText.append(partial);
                        callback.onPartialResponse(partial);
                    })
                    .onToolExecuted(toolExecution -> callback.onToolExecuted(formatToolExecution(toolExecution)))
                    .onCompleteResponse(response -> {
                        recordTokenUsage(appId, response == null ? null : response.tokenUsage());
                        try {
                            vueProjectBuildService.build(appId, callback::onBuildLog);
                            CodeQualityReport qualityReport = codeGenWorkflowExecutor.finalizeVueProject(appId);
                            callback.onQualityReport(qualityReport.summary());
                            callback.onComplete(fullText.toString());
                        } catch (Exception e) {
                            log.error("Vue 工程构建失败，appId={}", appId, e);
                            callback.onError(e.getMessage());
                        } finally {
                            vueProjectFileTool.clearAppId(appId);
                            vueProjectFileTool.clearStreamListener();
                        }
                    })
                    .onError(error -> {
                        vueProjectFileTool.clearAppId(appId);
                        vueProjectFileTool.clearStreamListener();
                        callback.onError(error.getMessage());
                    })
                    .start();
        } catch (Exception e) {
            vueProjectFileTool.clearAppId(appId);
            vueProjectFileTool.clearStreamListener();
            callback.onError(e.getMessage());
        }
    }

    /**
     * 记录 Vue Agent 调用的 Token 消耗（部分模型可能不返回用量，此时静默跳过）
     */
    private void recordTokenUsage(Long appId, TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return;
        }
        try {
            String model = aiModelRuntimeResolver.resolveScenario(AiModelScenario.VUE_AGENT.getConfigKey())
                    .getModelName();
            int input = tokenUsage.inputTokenCount() == null ? 0 : tokenUsage.inputTokenCount();
            int output = tokenUsage.outputTokenCount() == null ? 0 : tokenUsage.outputTokenCount();
            aiMetricsCollector.recordTokens(appId, CodeGenTypeEnum.VUE_PROJECT.getValue(), model, input, output);
        } catch (Exception e) {
            log.debug("记录 Token 用量失败：{}", e.getMessage());
        }
    }

    /**
     * 过滤工具调用 JSON，避免前端展示乱码
     */
    private boolean shouldSkipPartial(String partial) {
        if (StrUtil.isBlank(partial)) {
            return true;
        }
        String text = partial.trim();
        if (text.contains("\"path\"") && text.contains("\"content\"")) {
            return true;
        }
        if (text.startsWith("saveFile") && text.contains("{")) {
            return true;
        }
        if (text.startsWith("{") && text.contains("\"arguments\"")) {
            return true;
        }
        return false;
    }

    private String formatToolExecution(ToolExecution toolExecution) {
        if (toolExecution == null || toolExecution.request() == null) {
            return "tool";
        }
        String name = toolExecution.request().name();
        String args = toolExecution.request().arguments();
        if (StrUtil.isNotBlank(args) && JSONUtil.isTypeJSON(args)) {
            JSONObject obj = JSONUtil.parseObj(args);
            String path = obj.getStr("path");
            if (StrUtil.isNotBlank(path)) {
                return name + " → " + path;
            }
            String dir = obj.getStr("dir");
            if (StrUtil.isNotBlank(dir)) {
                return name + " → " + dir;
            }
        }
        if (StrUtil.isBlank(args)) {
            return name;
        }
        return name + " → " + StrUtil.maxLength(args, 80);
    }
}
