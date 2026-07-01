package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 模型接入配置（管理员）
 */
@Data
public class AiModelConfigVO implements Serializable {

    private String providerId;

    private String baseUrl;

    /**
     * 脱敏后的 API Key
     */
    private String apiKey;

    private Boolean apiKeyConfigured;

    private List<AiModelScenarioConfigVO> scenarios;
}
