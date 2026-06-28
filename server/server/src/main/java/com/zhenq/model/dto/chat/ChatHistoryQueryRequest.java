package com.zhenq.model.dto.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * 对话历史游标查询请求
 * <p>
 * 首次加载不传 lastId，返回最新 N 条；
 * 向前加载更多时传当前最早一条消息的 id 作为 lastId。
 */
@Data
public class ChatHistoryQueryRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 每页条数（默认 10，最大 20）
     */
    private Integer pageSize = 10;

    /**
     * 游标：当前已加载消息中最早一条的 id（向前翻页时使用）
     */
    private Long lastId;
}
