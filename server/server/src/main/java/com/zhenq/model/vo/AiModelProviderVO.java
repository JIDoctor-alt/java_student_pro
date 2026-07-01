package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 模型提供商（供前端接入配置）
 */
@Data
public class AiModelProviderVO implements Serializable {

    private String id;

    private String name;

    private String docsUrl;

    private String defaultBaseUrl;

    private List<AiModelOptionVO> models;
}
