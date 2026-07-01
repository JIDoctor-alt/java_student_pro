package com.zhenq.ai;

import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
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

    private AiCodeGeneratorService htmlDelegate;
    private AiCodeGeneratorService multiFileDelegate;
    private AiCodeGeneratorService retryDelegate;

    @PostConstruct
    void initDelegates() {
        htmlDelegate = buildDelegate(AiModelScenario.CODE_SYNC, AiModelScenario.HTML_STREAM);
        multiFileDelegate = buildDelegate(AiModelScenario.CODE_SYNC, AiModelScenario.MULTI_FILE_STREAM);
        retryDelegate = buildDelegate(AiModelScenario.CODE_RETRY, AiModelScenario.CODE_RETRY);
        log.info("AI 代码生成服务已按场景绑定模型（HTML / 多文件 / 重试）");
    }

    private AiCodeGeneratorService buildDelegate(AiModelScenario syncScenario, AiModelScenario streamScenario) {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(aiModelRouter.chatModel(syncScenario))
                .streamingChatModel(aiModelRouter.streamingChatModel(streamScenario))
                .build();
    }

    @Override
    public HtmlCodeResult generateHtmlCode(String userMessage) {
        return htmlDelegate.generateHtmlCode(userMessage);
    }

    @Override
    public MultiFileCodeResult generateMultiFileCode(String userMessage) {
        return multiFileDelegate.generateMultiFileCode(userMessage);
    }

    @Override
    public Flux<String> generateHtmlCodeStream(String userMessage) {
        return htmlDelegate.generateHtmlCodeStream(userMessage);
    }

    @Override
    public Flux<String> generateMultiFileCodeStream(String userMessage) {
        return multiFileDelegate.generateMultiFileCodeStream(userMessage);
    }

    /** 质量重试：使用低成本、低温度模型 */
    public HtmlCodeResult generateHtmlCodeForRetry(String userMessage) {
        return retryDelegate.generateHtmlCode(userMessage);
    }

    /** 质量重试：使用低成本、低温度模型 */
    public MultiFileCodeResult generateMultiFileCodeForRetry(String userMessage) {
        return retryDelegate.generateMultiFileCode(userMessage);
    }
}
