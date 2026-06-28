package com.zhenq.core.vue;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 修正 Vite 构建产物路径，使 dist 可在 /preview/.../dist/index.html 下正常加载静态资源
 */
@Slf4j
@Component
public class VueProjectPreviewPathFixer {

    /**
     * 构建前确保 vite.config 使用相对 base，避免资源指向站点根路径 /assets
     */
    public void patchViteConfigBeforeBuild(File projectDir) {
        File viteConfig = new File(projectDir, "vite.config.ts");
        if (!viteConfig.exists()) {
            return;
        }
        String content = FileUtil.readString(viteConfig, StandardCharsets.UTF_8);
        if (content.contains("base:")) {
            return;
        }
        String patched = content.replace(
                "export default defineConfig({",
                "export default defineConfig({\n  base: './',");
        if (!patched.equals(content)) {
            FileUtil.writeString(patched, viteConfig, StandardCharsets.UTF_8);
            log.info("已为 vite.config.ts 添加 base: './'");
        }
    }

    /**
     * 构建后修正 dist/index.html 中的绝对资源路径
     */
    public void patchDistIndexAfterBuild(File distDir) {
        File indexHtml = new File(distDir, "index.html");
        if (!indexHtml.exists()) {
            return;
        }
        String content = FileUtil.readString(indexHtml, StandardCharsets.UTF_8);
        String patched = content
                .replace("src=\"/assets/", "src=\"./assets/")
                .replace("href=\"/assets/", "href=\"./assets/")
                .replace("src='/assets/", "src='./assets/")
                .replace("href='/assets/", "href='./assets/")
                .replace("href=\"/vite.svg\"", "href=\"./vite.svg\"");
        if (!StrUtil.equals(content, patched)) {
            FileUtil.writeString(patched, indexHtml, StandardCharsets.UTF_8);
            log.info("已修正 dist/index.html 资源为相对路径");
        }
    }
}
