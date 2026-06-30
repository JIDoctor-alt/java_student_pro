package com.zhenq.core.quality;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zhenq.core.vue.VueProjectPathUtils;
import com.zhenq.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生成代码质量检测（结构完整性、可视化编辑标识、Vue 工程结构等）
 */
@Component
public class CodeQualityChecker {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<html[\\s>]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCTYPE_PATTERN = Pattern.compile("<!DOCTYPE\\s+html", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_CODE_ID_PATTERN = Pattern.compile("data-code-id\\s*=\\s*[\"'][^\"']+[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_BLOCK_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_BLOCK_PATTERN = Pattern.compile("```(?:javascript|js)\\s*\\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_BLOCK_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_SETUP_PATTERN = Pattern.compile("<script\\s+setup",
            Pattern.CASE_INSENSITIVE);

    public CodeQualityReport check(String generatedCode, CodeGenTypeEnum codeGenType) {
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            return CodeQualityReport.builder()
                    .passed(false)
                    .issues(List.of("Vue 工程请使用 checkVueProject(appId)"))
                    .build();
        }
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (StrUtil.isBlank(generatedCode)) {
            issues.add("生成内容为空");
            return CodeQualityReport.builder().passed(false).issues(issues).build();
        }
        String html = extractHtml(generatedCode, codeGenType);
        checkHtmlStructure(html, issues, warnings);
        if (codeGenType == CodeGenTypeEnum.MULTI_FILE) {
            if (!CSS_BLOCK_PATTERN.matcher(generatedCode).find()) {
                warnings.add("多文件模式未检测到 CSS 代码块");
            }
            if (!JS_BLOCK_PATTERN.matcher(generatedCode).find()) {
                warnings.add("多文件模式未检测到 JavaScript 代码块");
            }
        }
        boolean passed = issues.isEmpty();
        return CodeQualityReport.builder()
                .passed(passed)
                .issues(issues)
                .warnings(warnings)
                .build();
    }

    /**
     * Vue 工程构建完成后的质量检测（检查目录结构与构建产物）
     */
    public CodeQualityReport checkVueProject(Long appId) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        File projectDir = VueProjectPathUtils.getProjectDir(appId);
        if (!projectDir.isDirectory()) {
            issues.add("Vue 工程目录不存在");
            return buildReport(issues, warnings);
        }
        requireFile(projectDir, "package.json", issues);
        requireAnyFile(projectDir, List.of("vite.config.ts", "vite.config.js"), issues, "缺少 vite.config.ts/js");
        requireAnyFile(projectDir, List.of("index.html"), issues, "缺少 index.html");
        requireAnyFile(projectDir, List.of("src/main.ts", "src/main.js"), issues, "缺少 src/main.ts/js");
        requireAnyFile(projectDir, List.of("src/App.vue"), warnings, "缺少 src/App.vue");

        File distIndex = new File(VueProjectPathUtils.getDistDir(appId), "index.html");
        if (!distIndex.isFile()) {
            issues.add("构建产物 dist/index.html 不存在，预览不可用");
        }

        checkVueSourceFiles(projectDir, warnings);
        return buildReport(issues, warnings);
    }

    private void checkVueSourceFiles(File projectDir, List<String> warnings) {
        File srcDir = new File(projectDir, "src");
        if (!srcDir.isDirectory()) {
            warnings.add("缺少 src 目录");
            return;
        }
        List<File> vueFiles = FileUtil.loopFiles(srcDir, file -> file.getName().endsWith(".vue"));
        if (vueFiles.isEmpty()) {
            warnings.add("src 下未找到 .vue 组件文件");
            return;
        }
        long setupCount = vueFiles.stream()
                .filter(file -> SCRIPT_SETUP_PATTERN.matcher(FileUtil.readUtf8String(file)).find())
                .count();
        if (setupCount == 0) {
            warnings.add("未检测到 <script setup> 组合式 API 写法");
        }
    }

    private void checkHtmlStructure(String html, List<String> issues, List<String> warnings) {
        if (StrUtil.isBlank(html)) {
            issues.add("未检测到有效 HTML 结构");
            return;
        }
        if (!DOCTYPE_PATTERN.matcher(html).find() && !HTML_TAG_PATTERN.matcher(html).find()) {
            warnings.add("缺少 <!DOCTYPE html> 或 <html> 标签");
        }
        if (!html.toLowerCase().contains("<body")) {
            issues.add("HTML 缺少 body 结构");
        }
        Matcher idMatcher = DATA_CODE_ID_PATTERN.matcher(html);
        int idCount = 0;
        while (idMatcher.find()) {
            idCount++;
        }
        if (idCount == 0) {
            warnings.add("未检测到 data-code-id 属性，可视化点选编辑可能不可用");
        }
    }

    private void requireFile(File dir, String relativePath, List<String> issues) {
        if (!new File(dir, relativePath).isFile()) {
            issues.add("缺少必要文件: " + relativePath);
        }
    }

    private void requireAnyFile(File dir, List<String> candidates, List<String> target, String message) {
        boolean exists = candidates.stream().anyMatch(path -> new File(dir, path).isFile());
        if (!exists) {
            target.add(message);
        }
    }

    private CodeQualityReport buildReport(List<String> issues, List<String> warnings) {
        return CodeQualityReport.builder()
                .passed(issues.isEmpty())
                .issues(issues)
                .warnings(warnings)
                .build();
    }

    private String extractHtml(String content, CodeGenTypeEnum codeGenType) {
        Matcher blockMatcher = HTML_BLOCK_PATTERN.matcher(content);
        if (blockMatcher.find()) {
            return blockMatcher.group(1);
        }
        if (codeGenType == CodeGenTypeEnum.HTML) {
            return content.trim();
        }
        return "";
    }
}
