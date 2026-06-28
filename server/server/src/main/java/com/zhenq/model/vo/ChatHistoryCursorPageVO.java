package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 对话历史游标分页结果
 */
@Data
public class ChatHistoryCursorPageVO implements Serializable {

    /**
     * 消息列表（按时间正序：旧 → 新）
     */
    private List<ChatHistoryVO> records;

    /**
     * 是否还有更早的消息
     */
    private Boolean hasMore;

    /**
     * 下一页游标（当前批次中最早消息的 id，向前加载时传 lastId）
     */
    private Long nextCursor;
}
