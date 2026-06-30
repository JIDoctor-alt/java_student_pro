package com.zhenq.core.quality;

import cn.hutool.core.util.StrUtil;
import com.zhenq.model.enums.CodeGenTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 代码质量检测报告
 */
@Data
@Builder
public class CodeQualityReport {

    private boolean passed;

    @Builder.Default
    private List<String> issues = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public String summary() {
        if (passed && warnings.isEmpty()) {
            return "质量检测通过";
        }
        StringBuilder sb = new StringBuilder();
        if (!passed) {
            sb.append("未通过：").append(String.join("; ", issues));
        } else {
            sb.append("通过");
        }
        if (!warnings.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("警告：").append(String.join("; ", warnings));
        }
        return sb.toString();
    }
}
