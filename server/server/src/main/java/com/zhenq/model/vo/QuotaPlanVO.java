package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 套餐配置视图
 */
@Data
public class QuotaPlanVO implements Serializable {

    private String plan;

    private String label;

    private int dailyChatLimit;

    private int maxAppCount;

    private int priceYuan;

    private boolean current;
}
