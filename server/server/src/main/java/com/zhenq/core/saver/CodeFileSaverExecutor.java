package com.zhenq.core.saver;

import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.ai.model.MultiFileCodeResult;
import com.zhenq.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存门面（门面模式）
 * <p>
 * 对外提供统一的保存入口，屏蔽内部不同保存器（模板方法实现）的差异，
 * 调用方只需提供结果对象与生成类型，无需关心具体落盘细节。
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate HTML_SAVER = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate MULTI_FILE_SAVER = new MultiFileCodeFileSaverTemplate();

    private CodeFileSaverExecutor() {
    }

    /**
     * 根据生成类型选择对应保存器并保存代码
     *
     * @param codeResult 结构化输出结果（HtmlCodeResult / MultiFileCodeResult）
     * @param codeGenType 代码生成类型
     * @param appId 应用 id（用于目录命名，便于部署定位）
     * @return 保存后的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null) {
            throw new IllegalArgumentException("代码生成类型不能为空");
        }
        return switch (codeGenType) {
            case HTML -> HTML_SAVER.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> MULTI_FILE_SAVER.saveCode((MultiFileCodeResult) codeResult, appId);
            case VUE_PROJECT -> throw new IllegalArgumentException("Vue 工程模式由 Agent 工具落盘，不支持结构化保存");
        };
    }
}
