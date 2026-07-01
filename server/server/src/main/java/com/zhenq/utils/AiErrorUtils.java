package com.zhenq.utils;

/**
 * AI 调用错误信息转换
 */
public final class AiErrorUtils {

    private AiErrorUtils() {
    }

    public static String toUserMessage(Throwable error) {
        if (error == null) {
            return "生成失败";
        }
        return toUserMessage(error.getMessage());
    }

    public static String toUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return "生成失败，请稍后重试";
        }
        if (message.contains("Authentication Fails") || message.contains("Bearer sk-")) {
            return "AI API Key 未配置或无效。请设置 DEEPSEEK_API_KEY / DASHSCOPE_API_KEY，"
                    + "或创建 application-local.yml 后重启后端";
        }
        return message;
    }
}
