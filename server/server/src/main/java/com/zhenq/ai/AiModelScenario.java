package com.zhenq.ai;

import lombok.Getter;

/**
 * AI 模型使用场景（按场景选型以控制成本）
 */
@Getter
public enum AiModelScenario {

    /** 原生 HTML 流式生成（结构简单，优先低成本模型） */
    HTML_STREAM("html-stream", "HTML 流式生成"),

    /** 多文件流式生成 */
    MULTI_FILE_STREAM("multi-file-stream", "多文件流式生成"),

    /** 同步结构化输出（工作流重试、非流式落盘） */
    CODE_SYNC("code-sync", "同步结构化生成"),

    /** 质量检测失败后的修正重生成（低温度、低成本） */
    CODE_RETRY("code-retry", "质量修正重试"),

    /** Vue 工程 Agent（工具调用 + 多轮，可按复杂度切换模型） */
    VUE_AGENT("vue-agent", "Vue 工程 Agent"),

    /** 首页提示词优化（低成本、短文本） */
    PROMPT_OPTIMIZE("prompt-optimize", "提示词优化");

    private final String configKey;

    private final String description;

    AiModelScenario(String configKey, String description) {
        this.configKey = configKey;
        this.description = description;
    }
}
