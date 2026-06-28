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
}
