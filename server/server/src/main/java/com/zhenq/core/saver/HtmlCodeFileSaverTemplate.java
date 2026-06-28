package com.zhenq.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zhenq.ai.model.HtmlCodeResult;
import com.zhenq.model.enums.CodeGenTypeEnum;

/**
 * HTML 单文件保存器
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new IllegalArgumentException("HTML 代码内容不能为空");
        }
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
