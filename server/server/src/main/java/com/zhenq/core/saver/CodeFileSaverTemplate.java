package com.zhenq.core.saver;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zhenq.constant.AppConstant;
import com.zhenq.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 代码文件保存模板（模板方法模式）
 * <p>
 * 定义“创建唯一目录 -> 保存文件 -> 返回目录”的固定骨架，
 * 具体保存哪些文件由子类实现。
 *
 * @param <T> 结构化输出结果类型
 */
public abstract class CodeFileSaverTemplate<T> {

    /**
     * 模板方法：保存代码并返回保存目录
     *
     * @param result 结构化输出结果
     * @param appId  应用 id（非空时目录名为 类型_appId，便于部署定位；为空时使用雪花 ID）
     */
    public final File saveCode(T result, Long appId) {
        // 1. 校验输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        // 3. 保存具体文件（交给子类实现）
        saveFiles(result, baseDirPath);
        // 4. 返回保存目录
        return new File(baseDirPath);
    }

    /**
     * 输入校验，子类可重写以增强校验逻辑
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new IllegalArgumentException("代码结果对象不能为空");
        }
    }

    /**
     * 构建唯一保存目录：根目录/类型_appId（appId 为空时用雪花 ID）
     */
    protected final String buildUniqueDir(Long appId) {
        String codeType = getCodeType().getValue();
        String suffix = appId != null ? String.valueOf(appId) : IdUtil.getSnowflakeNextIdStr();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, suffix);
        String dirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + uniqueDirName;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dirPath;
    }

    /**
     * 写入单个文件
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {
        if (content == null) {
            content = "";
        }
        Path path = Paths.get(dirPath, fileName);
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("写入文件失败：" + path, e);
        }
    }

    /**
     * 当前保存器对应的代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存具体文件，由子类实现
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
