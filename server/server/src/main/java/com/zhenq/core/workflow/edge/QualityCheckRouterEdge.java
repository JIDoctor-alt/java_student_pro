package com.zhenq.core.workflow.edge;

import com.zhenq.config.AppProperties;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import com.zhenq.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 质量检测后的条件路由：通过则落盘，失败且未超重试次数则重试生成
 */
@Component
public class QualityCheckRouterEdge implements AsyncEdgeAction<CodeGenWorkflowState> {

    public static final String SAVE_CODE = "save_code";
    public static final String RETRY_GENERATE = "retry_generate";

    @Resource
    private AppProperties appProperties;

    @Override
    public CompletableFuture<String> apply(CodeGenWorkflowState state) {
        return CompletableFuture.completedFuture(route(state));
    }

    private String route(CodeGenWorkflowState state) {
        if (state.qualityPassed()) {
            return SAVE_CODE;
        }
        if (state.codeGenType() == CodeGenTypeEnum.VUE_PROJECT) {
            return SAVE_CODE;
        }
        if (state.retryCount() < appProperties.getWorkflowMaxRetries()) {
            return RETRY_GENERATE;
        }
        return SAVE_CODE;
    }
}
