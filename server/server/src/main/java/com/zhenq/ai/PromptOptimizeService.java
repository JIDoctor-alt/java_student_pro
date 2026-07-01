package com.zhenq.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 提示词优化 AI 服务
 */
public interface PromptOptimizeService {

    @SystemMessage(fromResource = "prompt/prompt-optimize-system-prompt.txt")
    String optimizePrompt(@UserMessage String userMessage);
}
