package com.zhenq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用自定义配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 部署后静态站点访问前缀
     */
    private String deployHost = "http://localhost:8123/api/static";

    /**
     * 本服务对外访问前缀（预览、封面等静态资源 URL 拼接用）
     */
    private String serverBaseUrl = "http://127.0.0.1:8123/api";

    /**
     * 是否启用 Selenium 自动生成应用封面
     */
    private boolean coverEnabled = true;

    /**
     * 封面截图宽度（像素）
     */
    private int coverWidth = 1280;

    /**
     * 封面截图高度（像素）
     */
    private int coverHeight = 800;

    /**
     * 页面加载后额外等待秒数（等待渲染完成）
     */
    private int coverWaitSeconds = 3;
}
