package com.zhenq.manager;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.zhenq.config.CosClientProperties;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;

/**
 * 腾讯云 COS 上传工具
 */
@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientProperties cosClientProperties;

    private volatile COSClient cosClient;

    public boolean isEnabled() {
        return cosClientProperties.isConfigured();
    }

    /**
     * 上传本地文件到 COS
     */
    public void putObject(String key, File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("上传文件不存在");
        }
        PutObjectRequest request = new PutObjectRequest(
                cosClientProperties.getBucket(), normalizeKey(key), file);
        getClient().putObject(request);
        log.info("COS 上传成功 key={} size={}", key, file.length());
    }

    /**
     * 上传输入流到 COS
     */
    public void putObject(String key, InputStream inputStream, long contentLength, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        if (StrUtil.isNotBlank(contentType)) {
            metadata.setContentType(contentType);
        }
        PutObjectRequest request = new PutObjectRequest(
                cosClientProperties.getBucket(), normalizeKey(key), inputStream, metadata);
        getClient().putObject(request);
        log.info("COS 上传成功 key={} size={}", key, contentLength);
    }

    /**
     * 构建对象对外访问 URL（基于配置的自定义域名）
     */
    public String getObjectUrl(String key) {
        String host = cosClientProperties.getHost().trim();
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "https://" + host;
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host + "/" + normalizeKey(key);
    }

    /**
     * 拼接带前缀的对象 key
     */
    public String buildObjectKey(String fileName) {
        String prefix = cosClientProperties.getKeyPrefix();
        if (StrUtil.isBlank(prefix)) {
            return fileName;
        }
        prefix = prefix.replace('\\', '/');
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + fileName;
    }

    private COSClient getClient() {
        if (!isEnabled()) {
            throw new IllegalStateException("COS 未配置，无法上传");
        }
        if (cosClient == null) {
            synchronized (this) {
                if (cosClient == null) {
                    COSCredentials cred = new BasicCOSCredentials(
                            cosClientProperties.getSecretId(), cosClientProperties.getSecretKey());
                    ClientConfig clientConfig = new ClientConfig(new Region(cosClientProperties.getRegion()));
                    cosClient = new COSClient(cred, clientConfig);
                }
            }
        }
        return cosClient;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        String normalized = key.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @PreDestroy
    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }
}
