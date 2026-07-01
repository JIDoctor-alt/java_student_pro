package com.zhenq.ai;

import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务（LangChain4j AiService）
 * <p>
 * 由 {@link RoutingAiCodeGeneratorService} 按场景注入不同 ChatModel，实现成本优化。
 * 结构化输出方法返回 POJO，由 LangChain4j 自动完成 JSON 解析；
 * 流式方法返回 {@link Flux}，逐 token 输出，提升用户体验。
 */
public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 单文件代码（同步，结构化输出）
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码（同步，结构化输出）
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 单文件代码（流式输出）
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码（流式输出）
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}
