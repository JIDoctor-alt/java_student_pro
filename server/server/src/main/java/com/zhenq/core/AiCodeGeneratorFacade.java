package com.zhenq.core;

import com.zhenq.ai.AiCodeGeneratorService;
import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;
import com.zhenq.core.parser.CodeParser;
import com.zhenq.core.saver.CodeFileSaverExecutor;
import com.zhenq.exception.BusinessException;
import com.zhenq.common.ErrorCode;
import com.zhenq.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.function.Function;

/**
 * AI 代码生成门面（门面模式）
 * <p>
 * 统一封装“调用 AI 生成 -> 解析 -> 落盘”的完整流程，
 * 对外屏蔽不同生成模式（HTML / 多文件）与同步/流式的差异。
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 生成代码并保存到本地（同步，结构化输出）
     *
     * @param userMessage 用户描述
     * @param codeGenType 生成模式
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "Vue 工程模式请使用应用对话接口");
        };
    }

    /**
     * 生成代码并保存到本地（流式输出）
     * <p>
     * 边流式返回 token，边累积完整内容；流结束后解析并落盘。
     *
     * @param userMessage 用户描述
     * @param codeGenType 生成模式
     * @return 流式 token
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenType) {
            case HTML -> processStream(
                    aiCodeGeneratorService.generateHtmlCodeStream(userMessage),
                    content -> CodeFileSaverExecutor.executeSaver(CodeParser.parseHtmlCode(content), CodeGenTypeEnum.HTML, appId));
            case MULTI_FILE -> processStream(
                    aiCodeGeneratorService.generateMultiFileCodeStream(userMessage),
                    content -> CodeFileSaverExecutor.executeSaver(CodeParser.parseMultiFileCode(content), CodeGenTypeEnum.MULTI_FILE, appId));
            case VUE_PROJECT -> Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR, "Vue 工程模式请使用应用对话接口"));
        };
    }

    /**
     * 累积流式内容，在流结束后解析并落盘
     *
     * @param stream AI 流式 token
     * @param saver  入参为完整内容，返回保存目录
     */
    private Flux<String> processStream(Flux<String> stream, Function<String, File> saver) {
        StringBuilder codeBuilder = new StringBuilder();
        Runnable saveCode = () -> {
            if (codeBuilder.isEmpty()) {
                return;
            }
            try {
                File dir = saver.apply(codeBuilder.toString());
                log.info("AI 代码已保存到目录：{}", dir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存代码失败", e);
            }
        };
        return stream
                .doOnNext(codeBuilder::append)
                .doOnComplete(saveCode)
                // 流异常中断时仍保存已生成的部分内容
                .doOnError(e -> {
                    log.warn("流式生成异常，尝试保存已生成内容：{}", e.getMessage());
                    saveCode.run();
                });
    }
}
