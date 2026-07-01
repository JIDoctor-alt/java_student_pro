package com.zhenq.model.dto.aimodel;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新 AI 模型接入配置
 */
@Data
public class AiModelConfigUpdateRequest implements Serializable {

    private String providerId;

    private String baseUrl;

    /**
     * 明文 API Key；留空或传脱敏值表示不修改
     */
    private String apiKey;

    private List<ScenarioUpdateItem> scenarios;

    @Data
    public static class ScenarioUpdateItem implements Serializable {
        private String scenarioKey;
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
