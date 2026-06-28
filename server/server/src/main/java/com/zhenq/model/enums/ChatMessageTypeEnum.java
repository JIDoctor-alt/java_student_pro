package com.zhenq.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;

/**
 * 对话消息类型
 */
@Getter
public enum ChatMessageTypeEnum {

    USER("用户消息", "user"),
    AI("AI 回复", "ai"),
    ERROR("错误信息", "error");

    private final String text;
    private final String value;

    ChatMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChatMessageTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        return Arrays.stream(values()).filter(e -> e.value.equals(value)).findFirst().orElse(null);
    }
}
