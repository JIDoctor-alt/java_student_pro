package com.zhenq.core.vue;

import cn.hutool.core.util.StrUtil;
import com.zhenq.common.ErrorCode;
import com.zhenq.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vue 工程项目构建（npm install + npm run build）
 */
@Slf4j
@Service
public class VueProjectBuildService {

    @Resource
    private VueProjectPackageNormalizer packageNormalizer;

    @Resource
    private VueProjectPreviewPathFixer previewPathFixer;

    public void build(Long appId, Consumer<String> logConsumer) {
        File projectDir = VueProjectPathUtils.getProjectDir(appId);
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "缺少 package.json，请先生成 Vue 工程文件");
        }
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法创建项目目录");
        }
        packageNormalizer.normalize(projectDir);
        previewPathFixer.patchViteConfigBeforeBuild(projectDir);
        emitLog(logConsumer, "已检查并修正 package.json / vite 配置");
        runNpm(projectDir, logConsumer, "install");
        runNpm(projectDir, logConsumer, "run", "build");
        File distDir = VueProjectPathUtils.getDistDir(appId);
        previewPathFixer.patchDistIndexAfterBuild(distDir);
        emitLog(logConsumer, "已修正预览资源路径（相对路径）");
        File distIndex = new File(distDir, "index.html");
        if (!distIndex.exists()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "构建完成但未找到 dist/index.html");
        }
        log.info("Vue 工程构建成功：{}", distIndex.getAbsolutePath());
    }

    private void runNpm(File projectDir, Consumer<String> logConsumer, String... npmArgs) {
        List<String> command = buildNpmCommand(npmArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(projectDir);
        processBuilder.redirectErrorStream(true);
        emitLog(logConsumer, "执行：" + String.join(" ", command));
        try {
            Process process = processBuilder.start();
            Charset charset = Charset.defaultCharset();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    emitLog(logConsumer, line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "npm 命令失败（exit=" + exitCode + "）：" + String.join(" ", npmArgs));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "执行 npm 失败：" + e.getMessage());
        }
    }

    private List<String> buildNpmCommand(String... npmArgs) {
        List<String> command = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            command.add("cmd");
            command.add("/c");
            command.add("npm");
        } else {
            command.add("npm");
        }
        for (String arg : npmArgs) {
            command.add(arg);
        }
        return command;
    }

    private void emitLog(Consumer<String> logConsumer, String line) {
        if (logConsumer != null && StrUtil.isNotBlank(line)) {
            logConsumer.accept(line);
        }
    }
}
