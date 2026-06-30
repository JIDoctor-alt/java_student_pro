package com.zhenq.core.visual;

import cn.hutool.core.util.StrUtil;
import com.zhenq.model.dto.visual.VisualEditContext;

/**
 * 可视化编辑 prompt 组装
 */
public final class VisualEditPromptUtils {

    private static final int SNIPPET_MAX = 2000;

    private VisualEditPromptUtils() {
    }

    /**
     * 对话历史中展示的用户消息（带点选摘要）
     */
    public static String formatUserMessage(String message, VisualEditContext ctx) {
        if (ctx == null || !ctx.hasSelection()) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[点选 ").append(ctx.getTagName());
        if (StrUtil.isNotBlank(ctx.getElementId())) {
            sb.append(" #").append(ctx.getElementId());
        }
        if (StrUtil.isNotBlank(ctx.getSourceFile())) {
            sb.append(" @ ").append(ctx.getSourceFile());
            if (ctx.getSourceLine() != null) {
                sb.append(":").append(ctx.getSourceLine());
            }
        }
        sb.append("] ").append(message);
        return sb.toString();
    }

    /**
     * 追加到 AI prompt 的可视化编辑段落
     */
    public static String appendVisualEditSection(String prompt, VisualEditContext ctx) {
        if (ctx == null || !ctx.hasSelection()) {
            return prompt;
        }
        StringBuilder sb = new StringBuilder(prompt);
        sb.append("\n\n【可视化编辑 - 仅修改用户选中的元素，禁止改动页面其他无关部分】\n");
        if (StrUtil.isNotBlank(ctx.getElementId())) {
            sb.append("- data-code-id: ").append(ctx.getElementId()).append("\n");
        }
        sb.append("- 元素标签: ").append(ctx.getTagName()).append("\n");
        if (StrUtil.isNotBlank(ctx.getCssSelector())) {
            sb.append("- CSS 选择器: ").append(ctx.getCssSelector()).append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getXpath())) {
            sb.append("- XPath: ").append(ctx.getXpath()).append("\n");
        }
        if (ctx.getSiblingIndex() != null) {
            sb.append("- 同级序号(从0起): ").append(ctx.getSiblingIndex()).append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getSourceFile())) {
            sb.append("- 源文件: ").append(ctx.getSourceFile());
            if (ctx.getSourceLine() != null) {
                sb.append(":").append(ctx.getSourceLine());
            }
            sb.append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getTextPreview())) {
            sb.append("- 文本摘要: ").append(StrUtil.maxLength(ctx.getTextPreview(), 200)).append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getOuterHtmlSnippet())) {
            sb.append("- HTML 片段:\n")
                    .append(StrUtil.maxLength(ctx.getOuterHtmlSnippet(), SNIPPET_MAX))
                    .append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getModificationDescription())) {
            sb.append("- 样式/属性修改描述: ").append(ctx.getModificationDescription()).append("\n");
        }
        sb.append("\n请精确定位上述元素并完成用户当前需求；若列表项共用模板，应通过数据或子组件区分，避免误改其他项。");
        return sb.toString();
    }
}
