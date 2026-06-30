package com.zhenq.core.cover;

import com.zhenq.model.enums.CodeGenTypeEnum;

/**
 * 应用预览 URL 工具
 */
public final class AppPreviewUrlUtils {

    private AppPreviewUrlUtils() {
    }

    /**
     * 构建供 Selenium 访问的预览页完整 URL
     */
    public static String buildPreviewPageUrl(String serverBaseUrl, String codeGenType, Long appId) {
        String base = trimTrailingSlash(serverBaseUrl);
        String type = codeGenType != null ? codeGenType : CodeGenTypeEnum.HTML.getValue();
        if (CodeGenTypeEnum.VUE_PROJECT.getValue().equals(type)) {
            return base + "/preview/vue_project_" + appId + "/dist/index.html";
        }
        return base + "/preview/" + type + "_" + appId + "/index.html";
    }

    /**
     * 构建封面图片对外访问 URL
     */
    public static String buildCoverPublicUrl(String serverBaseUrl, Long appId) {
        return trimTrailingSlash(serverBaseUrl) + "/cover/cover_" + appId + ".png";
    }

    /**
     * 封面文件本地路径（不含目录创建）
     */
    public static String buildCoverFileName(Long appId) {
        return "cover_" + appId + ".png";
    }

    /**
     * COS 对象 key（不含前缀，由 CosManager 拼接 keyPrefix）
     */
    public static String buildCoverObjectKey(Long appId) {
        return buildCoverFileName(appId);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://127.0.0.1:8123/api";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
