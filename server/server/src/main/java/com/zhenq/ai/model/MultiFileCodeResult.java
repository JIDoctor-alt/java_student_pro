package com.zhenq.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 原生多文件模式的结构化输出结果
 */
@Data
public class MultiFileCodeResult {

    @Description("HTML 代码，对应 index.html 文件内容")
    private String htmlCode;

    @Description("CSS 代码，对应 style.css 文件内容")
    private String cssCode;

    @Description("JavaScript 代码，对应 script.js 文件内容")
    private String jsCode;

    @Description("对生成的网页应用的简要描述")
    private String description;
}
