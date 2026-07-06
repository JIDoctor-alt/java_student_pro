package com.zhenq.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 提示词优化请求
 */
@Data
public class PromptOptimizeRequest implements Serializable {

    /**
     * 用户原始提示词
     */
    private String prompt;

    /**
     * 代码生成类型：html / multi_file / vue_project
     */
    private String codeGenType;
}
