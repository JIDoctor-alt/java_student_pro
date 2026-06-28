package com.zhenq.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用对话历史
 */
@Data
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 消息类型：user / ai / error
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送用户 id（用户消息时有值）
     */
    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
