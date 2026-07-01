package com.zhenq.model.enums;

import lombok.Getter;

/**
 * AI 模型接入提供商
 */
@Getter
public enum AiModelProviderEnum {

    DEEPSEEK(
            "deepseek",
            "DeepSeek",
            "https://api-docs.deepseek.com/zh-cn/",
            "https://api.deepseek.com/v1",
            new String[]{"deepseek-v4-flash", "deepseek-v4-pro", "deepseek-chat", "deepseek-reasoner"}
    );

    private final String id;
    private final String name;
    private final String docsUrl;
    private final String defaultBaseUrl;
    private final String[] models;

    AiModelProviderEnum(String id, String name, String docsUrl, String defaultBaseUrl, String[] models) {
        this.id = id;
        this.name = name;
        this.docsUrl = docsUrl;
        this.defaultBaseUrl = defaultBaseUrl;
        this.models = models;
    }

    public static AiModelProviderEnum getById(String id) {
        for (AiModelProviderEnum provider : values()) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return null;
    }
}
