package com.zhenq.core.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.zhenq.ai.AiCodeGeneratorService;
import com.zhenq.ai.model.MultiFileCodeResult;
import com.zhenq.core.quality.CodeQualityReport;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import com.zhenq.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流节点：质量检测失败后，携带问题反馈重新调用 AI 生成
 */
@Slf4j
@Component
public class RetryGenerateNode implements NodeAction<CodeGenWorkflowState> {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Override
    public Map<String, Object> apply(CodeGenWorkflowState state) {
        int attempt = state.retryCount() + 1;
        String fixPrompt = buildFixPrompt(state);
        log.info("[workflow] retry_generate -> attempt={}/{}", attempt, state.codeGenType());
        String newCode = regenerate(state.codeGenType(), fixPrompt);
        return Map.of(
                CodeGenWorkflowState.GENERATED_CODE, StrUtil.nullToEmpty(newCode),
                CodeGenWorkflowState.RETRY_COUNT, attempt,
                CodeGenWorkflowState.WORKFLOW_LOG,
                "[retry_generate] attempt=" + attempt + ", promptLength=" + fixPrompt.length()
        );
    }

    private String buildFixPrompt(CodeGenWorkflowState state) {
        String basePrompt = StrUtil.isNotBlank(state.enrichedPrompt())
                ? state.enrichedPrompt()
                : state.userMessage();
        CodeQualityReport report = state.qualityReport();
        if (report == null || report.isPassed()) {
            return basePrompt;
        }
        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append("\n\n【质量检测反馈】上次生成未通过，请修正以下问题后重新输出完整代码：\n");
        if (!report.getIssues().isEmpty()) {
            sb.append("- 必须修复：").append(String.join("；", report.getIssues())).append("\n");
        }
        if (!report.getWarnings().isEmpty()) {
            sb.append("- 建议改进：").append(String.join("；", report.getWarnings())).append("\n");
        }
        sb.append("请直接输出修正后的完整代码，不要解释。");
        return sb.toString();
    }

    private String regenerate(CodeGenTypeEnum type, String prompt) {
        return switch (type) {
            case HTML -> aiCodeGeneratorService.generateHtmlCode(prompt).getHtmlCode();
            case MULTI_FILE -> mergeMultiFileMarkdown(aiCodeGeneratorService.generateMultiFileCode(prompt));
            case VUE_PROJECT -> throw new IllegalStateException("Vue 工程不支持自动重试生成");
        };
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
