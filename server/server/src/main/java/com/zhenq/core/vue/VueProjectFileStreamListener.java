package com.zhenq.core.vue;

/**
 * Vue 工程文件工具流式回调（推送至 SSE）
 */
@FunctionalInterface
public interface VueProjectFileStreamListener {

    void onToolFile(ToolFileSsePayload payload);
}
