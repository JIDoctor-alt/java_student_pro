package com.zhenq.core;

import com.zhenq.ai.AiCodeGeneratorService;
import com.zhenq.core.workflow.CodeGenWorkflowExecutor;
import com.zhenq.exception.BusinessException;
import com.zhenq.common.ErrorCode;
import com.zhenq.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面（门面模式）
 * <p>
 * 统一封装 LangGraph4j 工作流（素材收集、质量检测）与 AI 生成、落盘流程，
 * 对外屏蔽不同生成模式（HTML / 多文件）与同步/流式的差异。
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private CodeGenWorkflowExecutor codeGenWorkflowExecutor;

    /**
     * 生成代码并保存到本地（同步，结构化输出）
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Vue 工程模式请使用应用对话接口");
        }
        return codeGenWorkflowExecutor.generateAndSave(userMessage, codeGenType, appId);
    }

    /**
     * 生成代码并保存到本地（流式输出）
     * <p>
     * 工作流在流式开始前收集素材并增强 Prompt；流结束后执行质量检测并落盘。
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenType) {
            case HTML -> processStream(
                    codeGenWorkflowExecutor.preparePrompt(userMessage, codeGenType, appId),
                    codeGenType,
                    appId,
                    prompt -> aiCodeGeneratorService.generateHtmlCodeStream(prompt));
            case MULTI_FILE -> processStream(
                    codeGenWorkflowExecutor.preparePrompt(userMessage, codeGenType, appId),
                    codeGenType,
                    appId,
                    prompt -> aiCodeGeneratorService.generateMultiFileCodeStream(prompt));
            case VUE_PROJECT -> Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR, "Vue 工程模式请使用应用对话接口"));
        };
    }

    private Flux<String> processStream(String enrichedPrompt,
                                       CodeGenTypeEnum codeGenType,
                                       Long appId,
                                       java.util.function.Function<String, Flux<String>> streamSupplier) {
        StringBuilder codeBuilder = new StringBuilder();
        Runnable saveCode = () -> {
            if (codeBuilder.isEmpty()) {
                return;
            }
            try {
                File dir = codeGenWorkflowExecutor.finalizeGeneration(
                        codeBuilder.toString(), codeGenType, appId);
                log.info("AI 代码已保存到目录：{}", dir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存代码失败", e);
            }
        };
        return streamSupplier.apply(enrichedPrompt)
                .doOnNext(codeBuilder::append)
                .doOnComplete(saveCode)
                .doOnError(e -> {
                    log.warn("流式生成异常，尝试保存已生成内容：{}", e.getMessage());
                    saveCode.run();
                });
    }
}
