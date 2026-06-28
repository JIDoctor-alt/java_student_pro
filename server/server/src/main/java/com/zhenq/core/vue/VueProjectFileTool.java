package com.zhenq.core.vue;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Vue 工程项目文件工具（LangChain4j Tool Calling）
 */
@Slf4j
@Component
public class VueProjectFileTool {

    private static final InheritableThreadLocal<Long> APP_ID = new InheritableThreadLocal<>();

    private static final ConcurrentMap<Long, Long> BOUND_APP_IDS = new ConcurrentHashMap<>();

    /**
     * 绑定当前生成任务的应用 id（需在 Agent 启动前调用）
     */
    public void bindAppId(Long appId) {
        BOUND_APP_IDS.put(appId, appId);
        APP_ID.set(appId);
        VueProjectContext.setAppId(appId);
    }

    public void clearAppId(Long appId) {
        if (appId != null) {
            BOUND_APP_IDS.remove(appId);
        }
        APP_ID.remove();
        VueProjectContext.clear();
    }

    public void clearAppId() {
        APP_ID.remove();
        VueProjectContext.clear();
    }

    @Tool("将代码保存到 Vue 工程项目指定路径，path 为相对项目根目录的路径")
    public String saveFile(@P("path") String path, @P("content") String content) {
        Long appId = requireAppId();
        Path target = resolveSafePath(appId, path);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            log.info("Vue 工程文件已保存：appId={}, path={}", appId, path);
            return "已保存：" + path;
        } catch (IOException e) {
            return "保存失败：" + path + "，" + e.getMessage();
        }
    }

    @Tool("读取 Vue 工程项目中已有文件内容，用于增量修改")
    public String readFile(@P("path") String path) {
        Long appId = requireAppId();
        Path target = resolveSafePath(appId, path);
        if (!Files.exists(target)) {
            return "文件不存在：" + path;
        }
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "读取失败：" + path + "，" + e.getMessage();
        }
    }

    @Tool("列出 Vue 工程项目目录下的文件（相对路径），dir 为空则列出项目根目录")
    public String listFiles(@P("dir") String dir) {
        Long appId = requireAppId();
        Path base = Paths.get(VueProjectPathUtils.getProjectDirPath(appId)).normalize();
        Path target = StrUtil.isBlank(dir) ? base : resolveSafePath(appId, dir);
        if (!Files.exists(target)) {
            return "目录不存在：" + (StrUtil.isBlank(dir) ? "/" : dir);
        }
        try (Stream<Path> stream = Files.walk(target)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> base.relativize(path.normalize()).toString().replace('\\', '/'))
                    .sorted()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "列出文件失败：" + e.getMessage();
        }
    }

    private Long requireAppId() {
        Long appId = APP_ID.get();
        if (appId == null) {
            appId = VueProjectContext.getAppId();
        }
        if (appId == null && BOUND_APP_IDS.size() == 1) {
            appId = BOUND_APP_IDS.keySet().iterator().next();
        }
        if (appId == null || appId <= 0) {
            throw new IllegalStateException("Vue 工程上下文 appId 未设置");
        }
        return appId;
    }

    private Path resolveSafePath(Long appId, String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            throw new IllegalArgumentException("path 不能为空");
        }
        Path base = Paths.get(VueProjectPathUtils.getProjectDirPath(appId)).normalize().toAbsolutePath();
        Path resolved = base.resolve(relativePath.replace('\\', '/')).normalize().toAbsolutePath();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("非法路径：" + relativePath);
        }
        return resolved;
    }
}
