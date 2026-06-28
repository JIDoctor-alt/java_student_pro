package com.zhenq.core.vue;

/**
 * Vue 工程项目生成上下文（ThreadLocal 传递 appId 给工具）
 */
public final class VueProjectContext {

    private static final InheritableThreadLocal<Long> APP_ID = new InheritableThreadLocal<>();

    private VueProjectContext() {
    }

    public static void setAppId(Long appId) {
        APP_ID.set(appId);
    }

    public static Long getAppId() {
        return APP_ID.get();
    }

    public static void clear() {
        APP_ID.remove();
    }
}
