package com.zhenq.ai;

import com.zhenq.config.AiModelProperties;
import com.zhenq.service.AiModelRuntimeResolver;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按场景路由 ChatModel / StreamingChatModel，实现大模型成本优化
 */
@Slf4j
@Component
public class AiModelRouter {

    @Resource
    private AiModelRuntimeResolver aiModelRuntimeResolver;

    private final Map<AiModelScenario, ChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<AiModelScenario, StreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    /**
     * 配置版本号；每次配置变更自增，供依赖的 AiService 判断是否需要重建
     */
    private final AtomicInteger configVersion = new AtomicInteger(0);

    public int configVersion() {
        return configVersion.get();
    }

    public void clearCache() {
        chatModelCache.clear();
        streamingModelCache.clear();
        configVersion.incrementAndGet();
        log.info("AI 模型缓存已清空，配置版本={}，下次调用将按最新配置重建", configVersion.get());
    }

    public ChatModel chatModel(AiModelScenario scenario) {
        return chatModelCache.computeIfAbsent(scenario, this::createChatModel);
    }

    public StreamingChatModel streamingChatModel(AiModelScenario scenario) {
        return streamingModelCache.computeIfAbsent(scenario, this::createStreamingChatModel);
    }

    private ChatModel createChatModel(AiModelScenario scenario) {
        String configKey = scenario.getConfigKey();
        AiModelProperties.ScenarioModel config = aiModelRuntimeResolver.resolveScenario(configKey);
        String baseUrl = aiModelRuntimeResolver.resolveBaseUrl(configKey);
        log.info("初始化 ChatModel 场景={} baseUrl={} model={} temperature={}",
                configKey, baseUrl, config.getModelName(), config.getTemperature());
        var builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(aiModelRuntimeResolver.resolveApiKey(configKey))
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses());
        applyMaxTokens(builder, aiModelRuntimeResolver.resolveMaxTokens(configKey));
        return builder.build();
    }

    private StreamingChatModel createStreamingChatModel(AiModelScenario scenario) {
        String configKey = scenario.getConfigKey();
        AiModelProperties.ScenarioModel config = aiModelRuntimeResolver.resolveScenario(configKey);
        String baseUrl = aiModelRuntimeResolver.resolveBaseUrl(configKey);
        log.info("初始化 StreamingChatModel 场景={} baseUrl={} model={} temperature={}",
                configKey, baseUrl, config.getModelName(), config.getTemperature());
        var builder = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(aiModelRuntimeResolver.resolveApiKey(configKey))
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses());
        applyMaxTokens(builder, aiModelRuntimeResolver.resolveMaxTokens(configKey));
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
