package com.zhenq.core.parser;

import cn.hutool.core.util.StrUtil;
import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析工具：从流式输出累积的 Markdown 文本中提取各类代码块。
 * 用于流式场景（流式返回的是原始文本，需要自行解析后再落盘）。
 */
public class CodeParser {

    private static final Pattern HTML_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final Pattern JS_PATTERN = Pattern.compile("```(?:javascript|js)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private CodeParser() {
    }

    /**
     * 解析 HTML 单文件代码
     */
    public static HtmlCodeResult parseHtmlCode(String content) {
        HtmlCodeResult result = new HtmlCodeResult();
        String html = extractFirst(HTML_PATTERN, content);
        // 没有代码块时，退化为使用原始内容
        result.setHtmlCode(StrUtil.isNotBlank(html) ? html : StrUtil.nullToEmpty(content).trim());
        return result;
    }

    /**
     * 解析多文件代码
     */
    public static MultiFileCodeResult parseMultiFileCode(String content) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(extractFirst(HTML_PATTERN, content));
        result.setCssCode(extractFirst(CSS_PATTERN, content));
        result.setJsCode(extractFirst(JS_PATTERN, content));
        return result;
    }

    private static String extractFirst(Pattern pattern, String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
