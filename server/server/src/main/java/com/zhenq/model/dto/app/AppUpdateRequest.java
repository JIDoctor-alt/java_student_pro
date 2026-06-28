package com.zhenq.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用请求（用户，仅能改自己的应用名称）
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;
}
