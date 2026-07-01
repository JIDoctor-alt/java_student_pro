package com.zhenq.ai;

import com.zhenq.core.vue.VueProjectFileTool;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Vue 工程 Agent 工厂：按配置版本懒重建，实现「保存模型配置后立即生效」
 */
@Slf4j
@Component
public class VueProjectAgentFactory {

    private static final String SYSTEM_PROMPT_PATH = "prompt/codegen-vue-project-system-prompt.txt";

    @Resource
    private AiModelRouter aiModelRouter;

    @Resource
    private VueProjectFileTool vueProjectFileTool;

    private volatile VueProjectAgentService cachedAgent;
    private volatile int builtVersion = -1;

    public synchronized VueProjectAgentService getAgent() {
        int currentVersion = aiModelRouter.configVersion();
        if (cachedAgent != null && builtVersion == currentVersion) {
            return cachedAgent;
        }
        cachedAgent = build();
        builtVersion = currentVersion;
        log.info("Vue 工程 Agent 已重建，配置版本={}", currentVersion);
        return cachedAgent;
    }

    private VueProjectAgentService build() {
        String systemPrompt = loadSystemPrompt();
        return AiServices.builder(VueProjectAgentService.class)
                .streamingChatModel(aiModelRouter.streamingChatModel(AiModelScenario.VUE_AGENT))
                .tools(vueProjectFileTool)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .build();
    }

    private String loadSystemPrompt() {
        try {
            return new ClassPathResource(SYSTEM_PROMPT_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("加载 Vue 工程系统提示词失败", e);
        }
    }
}
