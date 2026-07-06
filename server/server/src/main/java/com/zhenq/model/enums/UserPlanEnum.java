package com.zhenq.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;

/**
 * 用户套餐等级（参考 nocode 分级）
 */
@Getter
public enum UserPlanEnum {

    FREE("免费版", "free", 0),
    BASIC("基础版", "basic", 1),
    PRO("专业版", "pro", 2);

    private final String label;
    private final String value;
    private final int level;

    UserPlanEnum(String label, String value, int level) {
        this.label = label;
        this.value = value;
        this.level = level;
    }

    public static UserPlanEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return FREE;
        }
        return Arrays.stream(values())
                .filter(e -> e.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(FREE);
    }
}
