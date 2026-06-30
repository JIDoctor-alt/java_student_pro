package com.zhenq.core.workflow.node;

import com.zhenq.core.quality.CodeQualityChecker;
import com.zhenq.core.quality.CodeQualityReport;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流节点：代码质量检测
 */
@Slf4j
@Component
public class QualityCheckNode implements NodeAction<CodeGenWorkflowState> {

    @Resource
    private CodeQualityChecker codeQualityChecker;

    @Override
    public Map<String, Object> apply(CodeGenWorkflowState state) {
        CodeQualityReport report = codeQualityChecker.check(state.generatedCode(), state.codeGenType());
        log.info("[workflow] quality_check -> {}", report.summary());
        return Map.of(
                CodeGenWorkflowState.QUALITY_REPORT, report,
                CodeGenWorkflowState.QUALITY_PASSED, report.isPassed(),
                CodeGenWorkflowState.WORKFLOW_LOG, "[quality_check] " + report.summary()
        );
    }
}
