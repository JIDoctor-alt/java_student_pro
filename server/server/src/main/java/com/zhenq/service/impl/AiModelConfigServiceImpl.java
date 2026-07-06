package com.zhenq.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zhenq.ai.AiModelRouter;
import com.zhenq.ai.AiModelScenario;
import com.zhenq.common.ErrorCode;
import com.zhenq.config.AiModelProperties;
import com.zhenq.exception.BusinessException;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.model.dto.aimodel.AiModelConfigUpdateRequest;
import com.zhenq.model.dto.aimodel.AiModelRuntimeConfig;
import com.zhenq.model.enums.AiModelProviderEnum;
import com.zhenq.model.vo.AiModelConfigVO;
import com.zhenq.model.vo.AiModelOptionVO;
import com.zhenq.model.vo.AiModelProviderVO;
import com.zhenq.model.vo.AiModelScenarioConfigVO;
import com.zhenq.service.AiModelConfigService;
import com.zhenq.service.AiModelRuntimeResolver;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 模型接入配置：Redis 持久化 + YAML 默认值合并
 */
@Slf4j
@Service
public class AiModelConfigServiceImpl implements AiModelConfigService, AiModelRuntimeResolver {

    private static final String REDIS_KEY = "app:ai-model:runtime-config";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AiModelProperties aiModelProperties;

    @Lazy
    @Resource
    private AiModelRouter aiModelRouter;

    private final AtomicReference<AiModelRuntimeConfig> runtimeConfigRef = new AtomicReference<>();

    @PostConstruct
    void loadRuntimeConfig() {
        reloadFromRedis();
    }

    private void reloadFromRedis() {
        String json = stringRedisTemplate.opsForValue().get(REDIS_KEY);
        if (StrUtil.isBlank(json)) {
            runtimeConfigRef.set(null);
            return;
        }
        runtimeConfigRef.set(JSONUtil.toBean(json, AiModelRuntimeConfig.class));
        log.info("已加载 AI 模型运行时配置 provider={}", runtimeConfigRef.get().getProviderId());
    }

    @Override
    public List<AiModelProviderVO> listProviders() {
        List<AiModelProviderVO> list = new ArrayList<>();
        for (AiModelProviderEnum provider : AiModelProviderEnum.values()) {
            AiModelProviderVO vo = new AiModelProviderVO();
            vo.setId(provider.getId());
            vo.setName(provider.getName());
            vo.setDocsUrl(provider.getDocsUrl());
            vo.setDefaultBaseUrl(provider.getDefaultBaseUrl());
            List<AiModelOptionVO> models = new ArrayList<>();
            for (String modelId : provider.getModels()) {
                AiModelOptionVO option = new AiModelOptionVO();
                option.setId(modelId);
                option.setName(modelId);
                option.setDescription(describeModel(modelId));
                models.add(option);
            }
            vo.setModels(models);
            list.add(vo);
        }
        return list;
    }

    @Override
    public AiModelConfigVO getAdminConfig() {
        AiModelRuntimeConfig runtime = runtimeConfigRef.get();
        AiModelConfigVO vo = new AiModelConfigVO();
        if (runtime != null) {
            vo.setProviderId(runtime.getProviderId());
            vo.setBaseUrl(runtime.getBaseUrl());
            vo.setApiKey(maskApiKey(runtime.getApiKey()));
            vo.setApiKeyConfigured(StrUtil.isNotBlank(runtime.getApiKey()));
        } else {
            vo.setProviderId(AiModelProviderEnum.DEEPSEEK.getId());
            vo.setBaseUrl(aiModelProperties.getBaseUrl());
            vo.setApiKey(maskApiKey(aiModelProperties.getApiKey()));
            vo.setApiKeyConfigured(StrUtil.isNotBlank(aiModelProperties.getApiKey()));
        }
        vo.setScenarios(buildScenarioViews(runtime));
        return vo;
    }

    @Override
    public void saveAdminConfig(AiModelConfigUpdateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(request.getProviderId()), ErrorCode.PARAMS_ERROR, "请选择模型提供商");
        AiModelProviderEnum provider = AiModelProviderEnum.getById(request.getProviderId());
        ThrowUtils.throwIf(provider == null, ErrorCode.PARAMS_ERROR, "不支持的模型提供商");

        AiModelRuntimeConfig current = runtimeConfigRef.get();
        AiModelRuntimeConfig next = new AiModelRuntimeConfig();
        next.setProviderId(request.getProviderId());
        next.setBaseUrl(StrUtil.blankToDefault(request.getBaseUrl(), provider.getDefaultBaseUrl()));
        next.setApiKey(resolveApiKeyForSave(request.getApiKey(), current != null ? current.getApiKey() : null,
                aiModelProperties.getApiKey()));

        Map<String, AiModelRuntimeConfig.ScenarioRuntimeItem> scenarioMap = new LinkedHashMap<>();
        if (request.getScenarios() != null) {
            for (AiModelConfigUpdateRequest.ScenarioUpdateItem item : request.getScenarios()) {
                if (item == null || StrUtil.isBlank(item.getScenarioKey())) {
                    continue;
                }
                AiModelRuntimeConfig.ScenarioRuntimeItem scenario = new AiModelRuntimeConfig.ScenarioRuntimeItem();
                scenario.setProviderId(StrUtil.blankToDefault(item.getProviderId(), next.getProviderId()));
                scenario.setBaseUrl(StrUtil.blankToDefault(item.getBaseUrl(), next.getBaseUrl()));
                AiModelRuntimeConfig.ScenarioRuntimeItem oldScenario = current != null && current.getScenarios() != null
                        ? current.getScenarios().get(item.getScenarioKey()) : null;
                scenario.setApiKey(resolveApiKeyForSave(item.getApiKey(),
                        oldScenario != null ? oldScenario.getApiKey() : null, next.getApiKey()));
                scenario.setModelName(item.getModelName());
                scenario.setTemperature(item.getTemperature());
                scenario.setMaxTokens(item.getMaxTokens());
                scenario.setLogRequests(item.getLogRequests());
                scenario.setLogResponses(item.getLogResponses());
                scenarioMap.put(item.getScenarioKey(), scenario);
            }
        }
        next.setScenarios(scenarioMap);

        stringRedisTemplate.opsForValue().set(REDIS_KEY, JSONUtil.toJsonStr(next));
        runtimeConfigRef.set(next);
        aiModelRouter.clearCache();
        log.info("AI 模型配置已更新 provider={}", next.getProviderId());
    }

    @Override
    public void testConnection(String providerId, String baseUrl, String apiKey, String modelName) {
        AiModelProviderEnum provider = AiModelProviderEnum.getById(providerId);
        ThrowUtils.throwIf(provider == null, ErrorCode.PARAMS_ERROR, "不支持的模型提供商");
        String resolvedBaseUrl = StrUtil.blankToDefault(baseUrl, provider.getDefaultBaseUrl());
        String resolvedApiKey = resolveApiKeyForSave(apiKey,
                runtimeConfigRef.get() != null ? runtimeConfigRef.get().getApiKey() : null,
                aiModelProperties.getApiKey());
        ThrowUtils.throwIf(StrUtil.isBlank(resolvedApiKey), ErrorCode.PARAMS_ERROR, "请先配置 API Key");
        String resolvedModel = StrUtil.blankToDefault(modelName, provider.getModels()[0]);
        try {
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .baseUrl(resolvedBaseUrl)
                    .apiKey(resolvedApiKey)
                    .modelName(resolvedModel)
                    .maxTokens(64)
                    .logRequests(false)
                    .logResponses(false)
                    .build();
            // 连通性验证：只要能正常返回（不抛鉴权/模型错误）即视为成功
            model.chat("你好，请回复ok");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "连接失败：" + e.getMessage());
        }
    }

    @Override
    public AiModelProperties.ScenarioModel resolveScenario(String configKey) {
        AiModelRuntimeConfig.ScenarioRuntimeItem runtimeScenario = getRuntimeScenario(configKey);
        AiModelProperties.ScenarioModel yamlScenario = aiModelProperties.resolveScenario(configKey);
        if (runtimeScenario == null) {
            return yamlScenario;
        }
        AiModelProperties.ScenarioModel merged = new AiModelProperties.ScenarioModel();
        merged.setBaseUrl(firstNonBlank(runtimeScenario.getBaseUrl(), getRuntimeBaseUrl(), yamlScenario.getBaseUrl()));
        merged.setApiKey(firstNonBlank(runtimeScenario.getApiKey(), getRuntimeApiKey(), yamlScenario.getApiKey()));
        merged.setModelName(firstNonBlank(runtimeScenario.getModelName(), yamlScenario.getModelName(), "deepseek-v4-flash"));
        merged.setTemperature(runtimeScenario.getTemperature() != null ? runtimeScenario.getTemperature() : yamlScenario.getTemperature());
        merged.setMaxTokens(runtimeScenario.getMaxTokens() != null ? runtimeScenario.getMaxTokens() : yamlScenario.getMaxTokens());
        merged.setLogRequests(runtimeScenario.getLogRequests() != null ? runtimeScenario.getLogRequests() : yamlScenario.isLogRequests());
        merged.setLogResponses(runtimeScenario.getLogResponses() != null ? runtimeScenario.getLogResponses() : yamlScenario.isLogResponses());
        return merged;
    }

    @Override
    public String resolveBaseUrl(String configKey) {
        AiModelProperties.ScenarioModel scenario = resolveScenario(configKey);
        if (StrUtil.isNotBlank(scenario.getBaseUrl())) {
            return scenario.getBaseUrl();
        }
        return firstNonBlank(getRuntimeBaseUrl(), aiModelProperties.getBaseUrl());
    }

    @Override
    public String resolveApiKey(String configKey) {
        AiModelProperties.ScenarioModel scenario = resolveScenario(configKey);
        if (StrUtil.isNotBlank(scenario.getApiKey())) {
            return scenario.getApiKey();
        }
        return firstNonBlank(getRuntimeApiKey(), aiModelProperties.getApiKey());
    }

    @Override
    public Integer resolveMaxTokens(String configKey) {
        return resolveScenario(configKey).getMaxTokens();
    }

    private List<AiModelScenarioConfigVO> buildScenarioViews(AiModelRuntimeConfig runtime) {
        List<AiModelScenarioConfigVO> list = new ArrayList<>();
        for (AiModelScenario scenario : AiModelScenario.values()) {
            AiModelScenarioConfigVO vo = new AiModelScenarioConfigVO();
            vo.setScenarioKey(scenario.getConfigKey());
            vo.setScenarioName(scenario.getDescription());
            AiModelProperties.ScenarioModel merged = resolveScenario(scenario.getConfigKey());
            vo.setProviderId(runtime != null ? runtime.getProviderId() : AiModelProviderEnum.DEEPSEEK.getId());
            vo.setBaseUrl(merged.getBaseUrl());
            vo.setApiKey(maskApiKey(merged.getApiKey()));
            vo.setModelName(merged.getModelName());
            vo.setTemperature(merged.getTemperature());
            vo.setMaxTokens(merged.getMaxTokens());
            vo.setLogRequests(merged.isLogRequests());
            vo.setLogResponses(merged.isLogResponses());
            list.add(vo);
        }
        return list;
    }

    private AiModelRuntimeConfig.ScenarioRuntimeItem getRuntimeScenario(String configKey) {
        AiModelRuntimeConfig runtime = runtimeConfigRef.get();
        if (runtime == null || runtime.getScenarios() == null) {
            return null;
        }
        return runtime.getScenarios().get(configKey);
    }

    private String getRuntimeBaseUrl() {
        AiModelRuntimeConfig runtime = runtimeConfigRef.get();
        return runtime != null ? runtime.getBaseUrl() : null;
    }

    private String getRuntimeApiKey() {
        AiModelRuntimeConfig runtime = runtimeConfigRef.get();
        return runtime != null ? runtime.getApiKey() : null;
    }

    private static String resolveApiKeyForSave(String incoming, String previous, String fallback) {
        if (StrUtil.isBlank(incoming) || incoming.contains("****")) {
            if (StrUtil.isNotBlank(previous)) {
                return previous;
            }
            return StrUtil.blankToDefault(fallback, "");
        }
        return incoming.trim();
    }

    static String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String describeModel(String modelId) {
        return switch (modelId) {
            case "deepseek-v4-flash" -> "DeepSeek V4 快速版（推荐）";
            case "deepseek-v4-pro" -> "DeepSeek V4 专业版（更强）";
            case "deepseek-chat" -> "兼容旧名，将于 2026/07/24 弃用";
            case "deepseek-reasoner" -> "思考模式旧名，将于 2026/07/24 弃用";
            default -> modelId;
        };
    }
}
