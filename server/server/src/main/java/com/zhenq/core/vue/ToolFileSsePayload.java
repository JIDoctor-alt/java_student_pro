package com.zhenq.core.vue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE tool-file 事件载荷：向前端推送文件读写内容与流式分块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolFileSsePayload {

    /** 工具动作：save / read */
    private String action;

    /** 相对项目根目录的文件路径 */
    private String path;

    /** 本次追加的内容分块（前端累加展示） */
    private String chunk;

    /** 是否为本文件的最后一次推送 */
    private boolean done;

    /** 内容是否被截断（过大文件仅展示前 N 字符） */
    private boolean truncated;

    /** 展示类型：code / image / text */
    private String mediaType;
}
