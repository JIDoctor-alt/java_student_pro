package com.zhenq.config;

import com.zhenq.ai.AiModelRouter;
import com.zhenq.ai.AiModelScenario;
import com.zhenq.ai.PromptOptimizeService;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提示词优化 AI 服务配置
 */
@Configuration
public class PromptOptimizeConfig {

    @Resource
    private AiModelRouter aiModelRouter;

    @Bean
    public PromptOptimizeService promptOptimizeService() {
        return AiServices.builder(PromptOptimizeService.class)
                .chatModel(aiModelRouter.chatModel(AiModelScenario.PROMPT_OPTIMIZE))
                .build();
    }
}
