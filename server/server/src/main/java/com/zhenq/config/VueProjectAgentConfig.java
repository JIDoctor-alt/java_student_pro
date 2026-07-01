package com.zhenq.config;

import com.zhenq.ai.VueProjectAgentService;
import com.zhenq.ai.AiModelRouter;
import com.zhenq.ai.AiModelScenario;
import com.zhenq.core.vue.VueProjectFileTool;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Vue 工程 Agent 配置（独立注册工具，避免污染 HTML 生成 AiService）
 */
@Configuration
public class VueProjectAgentConfig {

    private static final String SYSTEM_PROMPT_PATH = "prompt/codegen-vue-project-system-prompt.txt";

    @Resource
    private AiModelRouter aiModelRouter;

    @Resource
    private VueProjectFileTool vueProjectFileTool;

    @Bean
    public VueProjectAgentService vueProjectAgentService() throws IOException {
        String systemPrompt = new ClassPathResource(SYSTEM_PROMPT_PATH).getContentAsString(StandardCharsets.UTF_8);
        return AiServices.builder(VueProjectAgentService.class)
                .streamingChatModel(aiModelRouter.streamingChatModel(AiModelScenario.VUE_AGENT))
                .tools(vueProjectFileTool)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .build();
    }
}
