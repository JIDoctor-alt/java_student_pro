package com.zhenq.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * AI 调用错误信息转换
 */
public final class AiErrorUtils {

    /** API Key 完全未配置 */
    public static final String API_KEY_NOT_CONFIGURED_MESSAGE =
            "AI 模型 API Key 未配置，请前往「模型接入」页面填写 API Key 并选择模型后再试";

    /** API Key 已填写但鉴权失败 */
    public static final String API_KEY_INVALID_MESSAGE =
            "AI 模型 API Key 无效或已过期，请前往「模型接入」页面检查并更新 API Key";

    /** @deprecated 使用 {@link #API_KEY_NOT_CONFIGURED_MESSAGE} */
    @Deprecated
    public static final String MODEL_NOT_CONFIGURED_MESSAGE = API_KEY_NOT_CONFIGURED_MESSAGE;

    private AiErrorUtils() {
    }

    public static String toUserMessage(Throwable error) {
        if (error == null) {
            return "生成失败，请稍后重试";
        }
        StringBuilder combined = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            if (StrUtil.isNotBlank(current.getMessage())) {
                if (combined.length() > 0) {
                    combined.append(' ');
                }
                combined.append(current.getMessage());
            }
            current = current.getCause();
        }
        String text = combined.length() > 0 ? combined.toString() : error.toString();
        return toUserMessage(text);
    }

    public static String toUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return "生成失败，请稍后重试";
        }
        String modelReason = resolveModelConfigReason(message);
        if (modelReason != null) {
            return modelReason;
        }
        if (looksLikeTechnicalError(message)) {
            return "生成失败，请稍后重试";
        }
        return message;
    }

    /**
     * 解析是否为模型 API Key 相关问题，并返回对应用户提示
     */
    public static String resolveModelConfigReason(String message) {
        if (StrUtil.isBlank(message)) {
            return null;
        }
        if (isApiKeyNotConfiguredSignal(message)) {
            return API_KEY_NOT_CONFIGURED_MESSAGE;
        }
        if (isApiKeyInvalidSignal(message)) {
            return API_KEY_INVALID_MESSAGE;
        }
        return null;
    }

    /**
     * @deprecated 使用 {@link #resolveModelConfigReason(String)}
     */
    @Deprecated
    public static boolean isModelConfigError(String message) {
        return resolveModelConfigReason(message) != null;
    }

    private static boolean isApiKeyNotConfiguredSignal(String message) {
        String lower = message.toLowerCase();
        return lower.contains("api key 未配置")
                || lower.contains("未配置 api key")
                || lower.contains("no api key provided")
                || lower.contains("you didn't provide an api key")
                || lower.contains("missing api key")
                || lower.contains("请先配置 api key")
                || lower.contains("deepseek_api_key")
                || lower.contains("dashscope_api_key");
    }

    private static boolean isApiKeyInvalidSignal(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("incorrect api key")
                || lower.contains("invalid_api_key")
                || lower.contains("invalidapikey")
                || lower.contains("apikey-error")
                || lower.contains("authentication fails")
                || lower.contains("invalid api key")
                || lower.contains("api key not valid")
                || lower.contains("unauthorized")
                || lower.contains("401")
                || lower.contains("403 forbidden")
                || lower.contains("invalid_request_error")
                || lower.contains("bearer sk-")) {
            return true;
        }
        return extractProviderErrorCode(message) != null;
    }

    /**
     * 从 OpenAI 兼容 / 阿里云等 JSON 错误体中提取鉴权相关 code
     */
    private static String extractProviderErrorCode(String message) {
        String json = extractJsonObject(message);
        if (json == null) {
            return null;
        }
        try {
            JSONObject root = JSONUtil.parseObj(json);
            JSONObject error = root.getJSONObject("error");
            if (error == null) {
                return null;
            }
            String code = StrUtil.blankToDefault(error.getStr("code"), "").toLowerCase();
            String errMsg = StrUtil.blankToDefault(error.getStr("message"), "").toLowerCase();
            if (code.contains("api_key") || code.contains("apikey") || code.contains("auth")) {
                return code;
            }
            if (errMsg.contains("api key") || errMsg.contains("apikey") || errMsg.contains("authentication")) {
                return code.isBlank() ? "auth_error" : code;
            }
        } catch (Exception ignored) {
            // ignore parse errors
        }
        return null;
    }

    private static String extractJsonObject(String message) {
        String trimmed = message.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        int start = message.indexOf('{');
        int end = message.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return message.substring(start, end + 1);
        }
        return null;
    }

    private static boolean looksLikeTechnicalError(String message) {
        String trimmed = message.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return extractProviderErrorCode(message) == null;
        }
        return trimmed.contains("\"error\"")
                || trimmed.contains("request_id")
                || trimmed.contains("Exception")
                || trimmed.contains("at com.")
                || trimmed.contains("at java.");
    }
}
