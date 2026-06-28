package com.zhenq.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zhenq.ai.model.MultiFileCodeResult;
import com.zhenq.model.enums.CodeGenTypeEnum;

/**
 * 多文件保存器：index.html / style.css / script.js
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new IllegalArgumentException("HTML 代码内容不能为空");
        }
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }
}
