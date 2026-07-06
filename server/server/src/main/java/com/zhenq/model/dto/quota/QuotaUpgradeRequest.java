package com.zhenq.model.dto.quota;

import lombok.Data;

import java.io.Serializable;

/**
 * 套餐升级请求（模拟支付）
 */
@Data
public class QuotaUpgradeRequest implements Serializable {

    /**
     * 目标套餐：basic / pro
     */
    private String plan;
}
