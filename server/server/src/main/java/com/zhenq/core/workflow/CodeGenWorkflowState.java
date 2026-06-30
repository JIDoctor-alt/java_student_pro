package com.zhenq.core.workflow;

import com.zhenq.core.asset.model.ImageAssetBundle;
import com.zhenq.core.quality.CodeQualityReport;
import com.zhenq.model.enums.CodeGenTypeEnum;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LangGraph4j 代码生成工作流共享状态
 */
public class CodeGenWorkflowState extends AgentState {

    public static final String USER_MESSAGE = "userMessage";
    public static final String CODE_GEN_TYPE = "codeGenType";
    public static final String APP_ID = "appId";
    public static final String IMAGE_ASSETS = "imageAssets";
    public static final String ENRICHED_PROMPT = "enrichedPrompt";
    public static final String GENERATED_CODE = "generatedCode";
    public static final String QUALITY_REPORT = "qualityReport";
    public static final String QUALITY_PASSED = "qualityPassed";
    public static final String RETRY_COUNT = "retryCount";
    public static final String WORKFLOW_LOG = "workflowLog";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            USER_MESSAGE, Channels.base(() -> ""),
            CODE_GEN_TYPE, Channels.base(() -> ""),
            APP_ID, Channels.base(() -> 0L),
            IMAGE_ASSETS, Channels.base(() -> null),
            ENRICHED_PROMPT, Channels.base(() -> ""),
            GENERATED_CODE, Channels.base(() -> ""),
            QUALITY_REPORT, Channels.base(() -> null),
            QUALITY_PASSED, Channels.base(() -> true),
            RETRY_COUNT, Channels.base(() -> 0),
            WORKFLOW_LOG, Channels.appender(ArrayList::new)
    );

    public CodeGenWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public String userMessage() {
        return this.<String>value(USER_MESSAGE).orElse("");
    }

    public CodeGenTypeEnum codeGenType() {
        String raw = this.<String>value(CODE_GEN_TYPE).orElse("");
        CodeGenTypeEnum type = CodeGenTypeEnum.getEnumByValue(raw);
        return type != null ? type : CodeGenTypeEnum.HTML;
    }

    public Long appId() {
        Object raw = value(APP_ID).orElse(0L);
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    public ImageAssetBundle imageAssets() {
        return this.<ImageAssetBundle>value(IMAGE_ASSETS).orElse(null);
    }

    public String enrichedPrompt() {
        return this.<String>value(ENRICHED_PROMPT).orElse("");
    }

    public String generatedCode() {
        return this.<String>value(GENERATED_CODE).orElse("");
    }

    public CodeQualityReport qualityReport() {
        return this.<CodeQualityReport>value(QUALITY_REPORT).orElse(null);
    }

    public boolean qualityPassed() {
        return this.<Boolean>value(QUALITY_PASSED).orElse(true);
    }

    public int retryCount() {
        Object raw = value(RETRY_COUNT).orElse(0);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    public List<String> workflowLog() {
        return this.<List<String>>value(WORKFLOW_LOG).orElse(List.of());
    }
}
