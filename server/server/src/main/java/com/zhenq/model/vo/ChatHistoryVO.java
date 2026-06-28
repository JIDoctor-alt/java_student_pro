package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图
 */
@Data
public class ChatHistoryVO implements Serializable {

    private Long id;

    private Long appId;

    /**
     * 消息类型：user / ai / error
     */
    private String messageType;

    private String content;

    private Long userId;

    private LocalDateTime createTime;
}
