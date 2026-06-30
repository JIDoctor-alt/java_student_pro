package com.zhenq.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查 DeepSeek API Key，避免调用 AI 时才报晦涩的鉴权错误
 */
@Slf4j
@Component
public class DeepSeekApiKeyValidator implements ApplicationRunner {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (StrUtil.isBlank(apiKey)) {
            log.error("DeepSeek API Key 未配置：请设置环境变量 DEEPSEEK_API_KEY，"
                    + "或复制 application-local.yml.example 为 application-local.yml 并填写 sk- 开头的密钥");
            return;
        }
        if (!apiKey.startsWith("sk-")) {
            log.warn("DeepSeek API Key 格式可能不正确（应以 sk- 开头），当前前缀: {}",
                    apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey);
        }
    }
}
