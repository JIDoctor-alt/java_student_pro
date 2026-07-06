package com.zhenq.model.dto.aimodel;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时 AI 模型配置（持久化到 Redis）
 */
@Data
public class AiModelRuntimeConfig implements Serializable {

    private String providerId = "deepseek";

    private String baseUrl = "https://api.deepseek.com/v1";

    private String apiKey = "";

    private Map<String, ScenarioRuntimeItem> scenarios = new LinkedHashMap<>();

    @Data
    public static class ScenarioRuntimeItem implements Serializable {
        private String providerId;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Double temperature;
        private Integer maxTokens;
        private Boolean logRequests;
        private Boolean logResponses;
    }
}
