package com.zhenq.ai;

import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 按场景路由底层模型的代码生成服务
 */
@Slf4j
@Primary
@Service
public class RoutingAiCodeGeneratorService implements AiCodeGeneratorService {

    @Resource
    private AiModelRouter aiModelRouter;

    private volatile AiCodeGeneratorService htmlDelegate;
    private volatile AiCodeGeneratorService multiFileDelegate;
    private volatile AiCodeGeneratorService retryDelegate;
    private volatile int builtVersion = -1;

    /**
     * 配置版本变化时重建委托，实现「保存配置后立即生效」
     */
    private synchronized void ensureDelegates() {
        int currentVersion = aiModelRouter.configVersion();
        if (builtVersion == currentVersion && htmlDelegate != null) {
            return;
        }
        htmlDelegate = buildDelegate(AiModelScenario.CODE_SYNC, AiModelScenario.HTML_STREAM);
        multiFileDelegate = buildDelegate(AiModelScenario.CODE_SYNC, AiModelScenario.MULTI_FILE_STREAM);
        retryDelegate = buildDelegate(AiModelScenario.CODE_RETRY, AiModelScenario.CODE_RETRY);
        builtVersion = currentVersion;
        log.info("AI 代码生成服务已按场景绑定模型（HTML / 多文件 / 重试），配置版本={}", currentVersion);
    }

    private AiCodeGeneratorService buildDelegate(AiModelScenario syncScenario, AiModelScenario streamScenario) {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(aiModelRouter.chatModel(syncScenario))
                .streamingChatModel(aiModelRouter.streamingChatModel(streamScenario))
                .build();
    }

    @Override
    public HtmlCodeResult generateHtmlCode(String userMessage) {
        ensureDelegates();
        return htmlDelegate.generateHtmlCode(userMessage);
    }

    @Override
    public MultiFileCodeResult generateMultiFileCode(String userMessage) {
        ensureDelegates();
        return multiFileDelegate.generateMultiFileCode(userMessage);
    }

    @Override
    public Flux<String> generateHtmlCodeStream(String userMessage) {
        ensureDelegates();
        return htmlDelegate.generateHtmlCodeStream(userMessage);
    }

    @Override
    public Flux<String> generateMultiFileCodeStream(String userMessage) {
        ensureDelegates();
        return multiFileDelegate.generateMultiFileCodeStream(userMessage);
    }

    /** 质量重试：使用低成本、低温度模型 */
    public HtmlCodeResult generateHtmlCodeForRetry(String userMessage) {
        ensureDelegates();
        return retryDelegate.generateHtmlCode(userMessage);
    }

    /** 质量重试：使用低成本、低温度模型 */
    public MultiFileCodeResult generateMultiFileCodeForRetry(String userMessage) {
        ensureDelegates();
        return retryDelegate.generateMultiFileCode(userMessage);
    }
}
