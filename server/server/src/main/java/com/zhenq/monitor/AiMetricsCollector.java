package com.zhenq.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 业务指标采集器（基于 Micrometer，导出到 Prometheus）。
 * <p>
 * 维度（Tag）：应用 ID、用户 ID、生成类型、模型、状态；
 * 指标（Metric）：请求次数、请求耗时、Token 消耗。
 * <p>
 * 注意：Tag 的取值应为有限集合，避免高基数（cardinality）导致时间序列爆炸。
 * 这里 appId / userId 在教学项目量级下可接受；如需上生产可考虑仅保留低基数维度。
 */
@Component
public class AiMetricsCollector {

    private static final String METRIC_REQUEST = "ai.request";
    private static final String METRIC_REQUEST_DURATION = "ai.request.duration";
    private static final String METRIC_TOKENS = "ai.tokens";

    private final MeterRegistry registry;

    public AiMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一次 AI 生成请求（次数 + 耗时）。
     *
     * @param appId      应用 ID
     * @param userId     用户 ID
     * @param genType    生成类型（html / multi_file / vue_project）
     * @param model      模型名称
     * @param status     结果状态（success / error）
     * @param durationMs 耗时（毫秒）
     */
    public void recordRequest(Long appId, Long userId, String genType, String model,
                              String status, long durationMs) {
        String appTag = safe(appId);
        String userTag = safe(userId);
        String typeTag = safe(genType);
        String modelTag = safe(model);
        String statusTag = safe(status);

        Counter.builder(METRIC_REQUEST)
                .tag("app_id", appTag)
                .tag("user_id", userTag)
                .tag("gen_type", typeTag)
                .tag("model", modelTag)
                .tag("status", statusTag)
                .description("AI 生成请求次数")
                .register(registry)
                .increment();

        Timer.builder(METRIC_REQUEST_DURATION)
                .tag("app_id", appTag)
                .tag("gen_type", typeTag)
                .tag("model", modelTag)
                .tag("status", statusTag)
                .description("AI 生成请求耗时")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录一次 AI 调用的 Token 消耗。
     *
     * @param appId        应用 ID
     * @param genType      生成类型
     * @param model        模型名称
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     */
    public void recordTokens(Long appId, String genType, String model,
                             int inputTokens, int outputTokens) {
        String appTag = safe(appId);
        String typeTag = safe(genType);
        String modelTag = safe(model);
        if (inputTokens > 0) {
            tokenCounter(appTag, typeTag, modelTag, "input").increment(inputTokens);
        }
        if (outputTokens > 0) {
            tokenCounter(appTag, typeTag, modelTag, "output").increment(outputTokens);
        }
        int total = Math.max(inputTokens, 0) + Math.max(outputTokens, 0);
        if (total > 0) {
            tokenCounter(appTag, typeTag, modelTag, "total").increment(total);
        }
    }

    private Counter tokenCounter(String appTag, String typeTag, String modelTag, String tokenType) {
        return Counter.builder(METRIC_TOKENS)
                .tag("app_id", appTag)
                .tag("gen_type", typeTag)
                .tag("model", modelTag)
                .tag("token_type", tokenType)
                .description("AI Token 消耗量")
                .register(registry);
    }

    private String safe(Object value) {
        if (value == null) {
            return "unknown";
        }
        String s = String.valueOf(value);
        return s.isBlank() ? "unknown" : s;
    }
}
