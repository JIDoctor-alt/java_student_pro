package com.zhenq.core.workflow;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.URLUtil;

import java.nio.charset.StandardCharsets;

/**
 * 工作流架构图工具（Mermaid / Kroki）
 * <p>
 * 可将工作流 Mermaid 源码转为可预览的图片 URL，便于文档与调试。
 */
public final class WorkflowDiagramUtils {

    private WorkflowDiagramUtils() {
    }

    /**
     * 代码生成工作流 Mermaid 源码
     */
    public static String codeGenWorkflowMermaid() {
        return """
                flowchart LR
                  START([START]) --> collect_assets[collect_assets\\nPexels + unDraw 并发]
                  collect_assets --> enrich_prompt[enrich_prompt\\n注入素材到 Prompt]
                  enrich_prompt --> generate[generate_code\\nLangChain4j 流式]
                  generate --> quality_check[quality_check\\n结构/ data-code-id 检测]
                  quality_check -->|通过| save[save_code\\n解析落盘]
                  quality_check -->|失败且未超重试| retry[retry_generate\\n携带反馈重生成]
                  retry --> quality_check
                  quality_check -->|失败且已达上限| save
                  save --> END([END])
                  subgraph vue [Vue 模式]
                    build[npm build] --> vue_qc[vue_quality_check\\n工程结构检测]
                  end
                """;
    }

    /**
     * 通过 mermaid.ink 生成 PNG 预览 URL
     * <p>
     * 文档：https://mermaid.ink/
     */
    public static String toMermaidInkPngUrl(String mermaidSource) {
        String encoded = Base64.encode(mermaidSource.getBytes(StandardCharsets.UTF_8));
        return "https://mermaid.ink/img/" + encoded;
    }

    /**
     * 通过 Kroki 生成 SVG 预览 URL
     * <p>
     * 文档：https://kroki.io/
     */
    public static String toKrokiSvgUrl(String mermaidSource) {
        return "https://kroki.io/mermaid/svg/" + URLUtil.encode(mermaidSource);
    }
}
