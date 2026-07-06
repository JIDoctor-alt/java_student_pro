package com.zhenq.ai;

import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 提示词优化服务工厂：按配置版本懒重建，实现「保存模型配置后立即生效」
 */
@Slf4j
@Component
public class PromptOptimizeFactory {

    @Resource
    private AiModelRouter aiModelRouter;

    private volatile PromptOptimizeService cachedService;
    private volatile int builtVersion = -1;

    public synchronized PromptOptimizeService getService() {
        int currentVersion = aiModelRouter.configVersion();
        if (cachedService != null && builtVersion == currentVersion) {
            return cachedService;
        }
        cachedService = AiServices.builder(PromptOptimizeService.class)
                .chatModel(aiModelRouter.chatModel(AiModelScenario.PROMPT_OPTIMIZE))
                .build();
        builtVersion = currentVersion;
        log.info("提示词优化服务已重建，配置版本={}", currentVersion);
        return cachedService;
    }
}
