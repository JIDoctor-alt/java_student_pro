package com.zhenq.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 对象存储配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cos.client")
public class CosClientProperties {

    /**
     * 自定义访问域名（CDN 或 COS 默认域名），如 https://cdn.example.com
     */
    private String host;

    private String secretId;

    private String secretKey;

    /**
     * 地域，如 ap-shanghai
     */
    private String region = "ap-shanghai";

    /**
     * 存储桶名称，如 bucket-1250000000
     */
    private String bucket;

    /**
     * 对象 key 前缀，默认 cover/
     */
    private String keyPrefix = "cover/";

    public boolean isConfigured() {
        return StrUtil.isNotBlank(host)
                && StrUtil.isNotBlank(secretId)
                && StrUtil.isNotBlank(secretKey)
                && StrUtil.isNotBlank(region)
                && StrUtil.isNotBlank(bucket);
    }
}
