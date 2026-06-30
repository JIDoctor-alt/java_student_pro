package com.zhenq.model.dto.visual;

import lombok.Data;

/**
 * 可视化编辑：用户点选预览元素时的上下文
 */
@Data
public class VisualEditContext {

    /**
     * 稳定锚点 data-code-id
     */
    private String elementId;

    private String tagName;

    private String cssSelector;

    private String xpath;

    private Integer siblingIndex;

    private String textPreview;

    private String outerHtmlSnippet;

    /**
     * 工程模式：源文件相对路径
     */
    private String sourceFile;

    private Integer sourceLine;

    private String pageUrl;

    /**
     * manual | chat
     */
    private String editMode;

    private String modificationDescription;

    public boolean hasSelection() {
        return tagName != null && !tagName.isBlank();
    }
}
