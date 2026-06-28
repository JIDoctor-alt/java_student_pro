package com.zhenq.core.vue;

/**
 * Vue 工程 Agent 流式回调（用于 SSE 推送）
 */
public interface VueProjectStreamCallback {

    void onPartialResponse(String partial);

    void onToolExecuted(String detail);

    void onBuildLog(String line);

    void onComplete(String fullText);

    void onError(String message);
}
