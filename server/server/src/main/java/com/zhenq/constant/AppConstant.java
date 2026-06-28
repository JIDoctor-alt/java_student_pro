package com.zhenq.constant;

import java.io.File;

/**
 * 应用通用常量
 */
public interface AppConstant {

    /**
     * AI 生成代码的本地落盘根目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";

    /**
     * 应用部署后的静态文件根目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_deploy";

    /**
     * 应用部署后的访问域名前缀（静态资源由后端 /static 提供）
     */
    String CODE_DEPLOY_HOST = "http://localhost:8123/api/static";

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;
}
