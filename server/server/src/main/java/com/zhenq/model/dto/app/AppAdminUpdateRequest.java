package com.zhenq.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用请求（管理员，可改名称/封面/优先级）
 */
@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级（精选为 99）
     */
    private Integer priority;
}
