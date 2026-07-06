package com.zhenq.controller;

import cn.hutool.core.util.StrUtil;
import com.zhenq.common.BaseResponse;
import com.zhenq.common.ErrorCode;
import com.zhenq.common.ResultUtils;
import com.zhenq.core.AiCodeGeneratorFacade;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.model.enums.CodeGenTypeEnum;
import com.zhenq.utils.AiErrorUtils;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 网页应用生成接口
 */
@RestController
@RequestMapping("/ai")
public class AiCodeGeneratorController {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    /**
     * 同步生成网页代码并落盘，返回保存目录
     *
     * @param prompt      用户描述
     * @param codeGenType 生成模式：html / multi_file
     */
    @GetMapping("/generate")
    public BaseResponse<String> generateCode(@RequestParam String prompt,
                                             @RequestParam(defaultValue = "html") String codeGenType) {
        ThrowUtils.throwIf(StrUtil.isBlank(prompt), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的生成类型");
        File dir = aiCodeGeneratorFacade.generateAndSaveCode(prompt, typeEnum, null);
        return ResultUtils.success(dir.getAbsolutePath());
    }

    /**
     * SSE 流式生成网页代码（逐 token 输出），生成结束后自动落盘
     *
     * @param prompt      用户描述
     * @param codeGenType 生成模式：html / multi_file
     */
    @GetMapping(value = "/generate/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateCodeStream(@RequestParam String prompt,
                                                            @RequestParam(defaultValue = "html") String codeGenType) {
        ThrowUtils.throwIf(StrUtil.isBlank(prompt), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的生成类型");
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(prompt, typeEnum, null)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("gen-error")
                        .data(AiErrorUtils.toUserMessage(e))
                        .build()))
                .concatWith(Flux.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build()));
    }
}
