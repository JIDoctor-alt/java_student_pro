package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 可选模型项
 */
@Data
public class AiModelOptionVO implements Serializable {

    private String id;

    private String name;

    private String description;
}
