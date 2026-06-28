package com.zhenq.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建应用请求（用户）
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt（必填，用户对应用的描述）
     */
    private String initPrompt;

    /**
     * 代码生成类型：html / multi_file / vue_project（默认 html）
     */
    private String codeGenType;
}
