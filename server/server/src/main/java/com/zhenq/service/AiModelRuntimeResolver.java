package com.zhenq.service;

import com.zhenq.config.AiModelProperties;

/**
 * 运行时 AI 模型参数解析（YAML + Redis 覆盖）
 */
public interface AiModelRuntimeResolver {

    AiModelProperties.ScenarioModel resolveScenario(String configKey);

    String resolveBaseUrl(String configKey);

    String resolveApiKey(String configKey);

    Integer resolveMaxTokens(String configKey);
}
