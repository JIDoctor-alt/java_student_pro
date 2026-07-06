package com.zhenq.model.dto.quota;

import lombok.Data;

import java.io.Serializable;

/**
 * 付费扩容请求（模拟支付，生产需对接支付网关）
 */
@Data
public class QuotaExpandRequest implements Serializable {

    /**
     * 扩容类型：chat / app
     */
    private String type;

    /**
     * 购买包数，默认 1
     */
    private Integer packs;
}
