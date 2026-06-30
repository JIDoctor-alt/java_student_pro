package com.zhenq.core.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.zhenq.core.asset.model.ImageAssetBundle;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流节点：将素材信息注入 Prompt
 */
@Slf4j
@Component
public class EnrichPromptNode implements NodeAction<CodeGenWorkflowState> {

    @Override
    public Map<String, Object> apply(CodeGenWorkflowState state) {
        String userMessage = state.userMessage();
        ImageAssetBundle bundle = state.imageAssets();
        String enriched = userMessage;
        if (bundle != null && !bundle.isEmpty()) {
            enriched = userMessage + bundle.toPromptBlock();
        }
        log.info("[workflow] enrich_prompt -> length={}", enriched.length());
        return Map.of(
                CodeGenWorkflowState.ENRICHED_PROMPT, enriched,
                CodeGenWorkflowState.WORKFLOW_LOG, "[enrich_prompt] promptLength=" + enriched.length()
        );
    }
}
