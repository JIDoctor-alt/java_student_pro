package com.zhenq.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 原生 HTML 模式的结构化输出结果
 */
@Data
public class HtmlCodeResult {

    @Description("生成的 HTML 代码（包含内联的 CSS 与 JS）")
    private String htmlCode;

    @Description("对生成的网页应用的简要描述")
    private String description;
}
