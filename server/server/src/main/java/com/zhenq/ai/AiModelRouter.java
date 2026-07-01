package com.zhenq.ai;

import com.zhenq.config.AiModelProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按场景路由 ChatModel / StreamingChatModel，实现大模型成本优化
 */
@Slf4j
@Component
public class AiModelRouter {

    @Resource
    private AiModelProperties aiModelProperties;

    private final Map<AiModelScenario, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<AiModelScenario, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    public ChatModel chatModel(AiModelScenario scenario) {
        return chatModelCache.computeIfAbsent(scenario, this::createChatModel);
    }

    public StreamingChatModel streamingChatModel(AiModelScenario scenario) {
        return streamingModelCache.computeIfAbsent(scenario, this::createStreamingChatModel);
    }

    private ChatModel createChatModel(AiModelScenario scenario) {
        String configKey = scenario.getConfigKey();
        AiModelProperties.ScenarioModel config = aiModelProperties.resolveScenario(configKey);
        String baseUrl = aiModelProperties.resolveBaseUrl(configKey);
        log.info("初始化 ChatModel 场景={} baseUrl={} model={} temperature={}",
                configKey, baseUrl, config.getModelName(), config.getTemperature());
        var builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(aiModelProperties.resolveApiKey(configKey))
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses());
        applyMaxTokens(builder, aiModelProperties.resolveMaxTokens(configKey));
        return builder.build();
    }

    private StreamingChatModel createStreamingChatModel(AiModelScenario scenario) {
        String configKey = scenario.getConfigKey();
        AiModelProperties.ScenarioModel config = aiModelProperties.resolveScenario(configKey);
        String baseUrl = aiModelProperties.resolveBaseUrl(configKey);
        log.info("初始化 StreamingChatModel 场景={} baseUrl={} model={} temperature={}",
                configKey, baseUrl, config.getModelName(), config.getTemperature());
        var builder = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(aiModelProperties.resolveApiKey(configKey))
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses());
        applyMaxTokens(builder, aiModelProperties.resolveMaxTokens(configKey));
        return builder.build();
    }

    private static void applyMaxTokens(Object builder, Integer maxTokens) {
        if (maxTokens == null) {
            return;
        }
        if (builder instanceof OpenAiChatModel.OpenAiChatModelBuilder chatBuilder) {
            chatBuilder.maxTokens(maxTokens);
        } else if (builder instanceof OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder streamBuilder) {
            streamBuilder.maxTokens(maxTokens);
        }
    }
}
