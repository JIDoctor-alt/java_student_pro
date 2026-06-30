package com.zhenq.core.vue;

/**
 * Vue 工程 Agent 流式回调（用于 SSE 推送）
 */
public interface VueProjectStreamCallback {

    void onPartialResponse(String partial);

    void onToolExecuted(String detail);

    void onBuildLog(String line);

    void onComplete(String fullText);

    /**
     * 构建完成后的质量检测报告（通过 SSE quality-report 推送）
     */
    default void onQualityReport(String summary) {
    }

    void onError(String message);
}
