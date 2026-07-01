package com.zhenq.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分场景 AI 模型配置（成本优化：不同任务使用不同模型/温度）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai-model")
public class AiModelProperties {

    /**
     * OpenAI 兼容 API 地址（DeepSeek 等）
     */
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * API Key，默认读取 DEEPSEEK_API_KEY
     */
    private String apiKey = "";

    /**
     * 各场景模型参数，key 见 {@link com.zhenq.ai.AiModelScenario#getConfigKey()}
     */
    private Map<String, ScenarioModel> scenarios = defaultScenarios();

    @Data
    public static class ScenarioModel {
        /** 可选：覆盖全局 base-url（如 DashScope 兼容模式） */
        private String baseUrl;
        /** 可选：覆盖全局 api-key（如 DASHSCOPE_API_KEY） */
        private String apiKey;
        private String modelName = "deepseek-chat";
        private double temperature = 0.7;
        /** 可选：限制单次回复 token 数（代码生成建议 4096+） */
        private Integer maxTokens;
        private boolean logRequests = false;
        private boolean logResponses = false;
    }

    public ScenarioModel resolveScenario(String configKey) {
        ScenarioModel configured = scenarios.get(configKey);
        if (configured != null) {
            return configured;
        }
        ScenarioModel fallback = new ScenarioModel();
        fallback.setModelName("deepseek-chat");
        fallback.setTemperature(0.7);
        return fallback;
    }

    public String resolveBaseUrl(String configKey) {
        ScenarioModel scenario = resolveScenario(configKey);
        if (StrUtil.isNotBlank(scenario.getBaseUrl())) {
            return scenario.getBaseUrl();
        }
        return baseUrl;
    }

    public String resolveApiKey(String configKey) {
        ScenarioModel scenario = resolveScenario(configKey);
        if (StrUtil.isNotBlank(scenario.getApiKey())) {
            return scenario.getApiKey();
        }
        return apiKey;
    }

    public Integer resolveMaxTokens(String configKey) {
        return resolveScenario(configKey).getMaxTokens();
    }

    public boolean hasAnyApiKeyConfigured() {
        if (StrUtil.isNotBlank(apiKey)) {
            return true;
        }
        for (ScenarioModel scenario : scenarios.values()) {
            if (scenario != null && StrUtil.isNotBlank(scenario.getApiKey())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, ScenarioModel> defaultScenarios() {
        Map<String, ScenarioModel> map = new LinkedHashMap<>();
        map.put("html-stream", scenario("deepseek-v4-flash", 0.7));
        map.put("multi-file-stream", scenario("deepseek-v4-flash", 0.7));
        map.put("code-sync", scenario("deepseek-v4-flash", 0.7));
        map.put("code-retry", scenario("deepseek-v4-flash", 0.3));
        map.put("vue-agent", scenario("deepseek-v4-pro", 0.5));
        map.put("prompt-optimize", scenario("qwen-turbo", 0.7));
        return map;
    }

    private static ScenarioModel scenario(String modelName, double temperature) {
        ScenarioModel model = new ScenarioModel();
        model.setModelName(modelName);
        model.setTemperature(temperature);
        return model;
    }
}
