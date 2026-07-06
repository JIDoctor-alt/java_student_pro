package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 分场景模型配置（前端展示）
 */
@Data
public class AiModelScenarioConfigVO implements Serializable {

    private String scenarioKey;

    private String scenarioName;

    private String providerId;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private Boolean logRequests;

    private Boolean logResponses;
}
