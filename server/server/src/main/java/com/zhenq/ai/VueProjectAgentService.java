package com.zhenq.ai;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Vue3 工程 Agent（工具调用 + 流式输出）
 */
public interface VueProjectAgentService {

    TokenStream generateVueProjectStream(@UserMessage String userMessage);
}
