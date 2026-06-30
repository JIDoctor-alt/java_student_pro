package com.zhenq.core.workflow.node;

import com.zhenq.core.asset.ImageAssetCollector;
import com.zhenq.core.asset.model.ImageAssetBundle;
import com.zhenq.core.workflow.CodeGenWorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流节点：并发收集 Pexels 照片与 unDraw 插画
 */
@Slf4j
@Component
public class CollectAssetsNode implements NodeAction<CodeGenWorkflowState> {

    @Resource
    private ImageAssetCollector imageAssetCollector;

    @Override
    public Map<String, Object> apply(CodeGenWorkflowState state) {
        ImageAssetBundle bundle = imageAssetCollector.collect(state.userMessage());
        log.info("[workflow] collect_assets -> {}", bundle.summary());
        return Map.of(
                CodeGenWorkflowState.IMAGE_ASSETS, bundle,
                CodeGenWorkflowState.WORKFLOW_LOG, "[collect_assets] " + bundle.summary()
        );
    }
}
