package com.zhenq.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查 AI API Key（DeepSeek / DashScope 等），避免调用 AI 时才报晦涩的鉴权错误
 */
@Slf4j
@Component
public class DeepSeekApiKeyValidator implements ApplicationRunner {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String legacyApiKey;

    @Resource
    private AiModelProperties aiModelProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (aiModelProperties.hasAnyApiKeyConfigured() || StrUtil.isNotBlank(legacyApiKey)) {
            return;
        }
        log.error("AI API Key 未配置：请设置 DEEPSEEK_API_KEY 和/或 DASHSCOPE_API_KEY，"
                + "或复制 application-local.yml.example 为 application-local.yml 并填写密钥");
    }
}
