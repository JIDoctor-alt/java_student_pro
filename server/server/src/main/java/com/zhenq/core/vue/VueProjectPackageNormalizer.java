package com.zhenq.core.vue;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 修正 AI 生成的 package.json 中不存在的依赖版本
 */
@Slf4j
@Component
public class VueProjectPackageNormalizer {

    /**
     * 已知 AI 容易写错、需强制修正的版本
     */
    private static final Map<String, String> FIX_VERSIONS = Map.of(
            "@ant-design/icons-vue", "^7.0.1"
    );

    public void normalize(File projectDir) {
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            return;
        }
        String raw = FileUtil.readString(packageJsonFile, StandardCharsets.UTF_8);
        String fixed = raw.replace("\"@ant-design/icons-vue\": \"^7.0.2\"", "\"@ant-design/icons-vue\": \"^7.0.1\"")
                .replace("\"@ant-design/icons-vue\":\"^7.0.2\"", "\"@ant-design/icons-vue\":\"^7.0.1\"");
        if (!JSONUtil.isTypeJSON(fixed)) {
            return;
        }
        JSONObject root = JSONUtil.parseObj(fixed);
        boolean changed = !fixed.equals(raw);

        changed |= fixDeps(root, "dependencies");
        changed |= fixDeps(root, "devDependencies");

        JSONObject scripts = root.getJSONObject("scripts");
        if (scripts != null && scripts.containsKey("build")) {
            String build = scripts.getStr("build");
            if (build != null && build.contains("vue-tsc")) {
                scripts.set("build", "vite build");
                changed = true;
            }
        }

        if (changed) {
            FileUtil.writeString(JSONUtil.toJsonPrettyStr(root), packageJsonFile, StandardCharsets.UTF_8);
            log.info("已修正 package.json：{}", packageJsonFile.getAbsolutePath());
        }
    }

    private boolean fixDeps(JSONObject root, String key) {
        JSONObject deps = root.getJSONObject(key);
        if (deps == null) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, String> entry : FIX_VERSIONS.entrySet()) {
            if (deps.containsKey(entry.getKey())) {
                String current = deps.getStr(entry.getKey());
                if (current != null && !entry.getValue().equals(current)) {
                    deps.set(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        return changed;
    }
}
