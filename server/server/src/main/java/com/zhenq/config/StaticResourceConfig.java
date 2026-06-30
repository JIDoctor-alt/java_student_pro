package com.zhenq.config;

import com.zhenq.constant.AppConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;

/**
 * 应用静态资源访问配置
 * <p>
 * - /static/**  映射到已部署目录（按 deployKey 访问，对应「应用部署」）
 * - /preview/** 映射到生成目录（按 类型_appId 访问，对应「实时查看应用效果」）
 * 访问目录时自动返回该目录下的 index.html。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator)
                .resourceChain(true)
                .addResolver(indexFallbackResolver());

        registry.addResourceHandler("/preview/**")
                .addResourceLocations("file:" + AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator)
                .resourceChain(true)
                .addResolver(indexFallbackResolver());

        registry.addResourceHandler("/cover/**")
                .addResourceLocations("file:" + AppConstant.CODE_COVER_ROOT_DIR + File.separator);
    }

    /**
     * 访问目录时回退到该目录下的 index.html
     */
    private PathResourceResolver indexFallbackResolver() {
        return new PathResourceResolver() {
            @Override
            protected Resource getResource(String resourcePath, Resource location) throws java.io.IOException {
                if (resourcePath == null || resourcePath.isBlank()) {
                    return null;
                }
                // 去掉首尾斜杠，统一路径格式（兼容 Windows）
                String normalized = resourcePath.replace('\\', '/');
                while (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                while (normalized.endsWith("/")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                if (normalized.isBlank()) {
                    return null;
                }
                // 1. 直接匹配文件（如 html_2/index.html）
                Resource fileResource = location.createRelative(normalized);
                if (fileResource.exists() && fileResource.isReadable()) {
                    try {
                        if (!fileResource.getFile().isDirectory()) {
                            return fileResource;
                        }
                    } catch (Exception ignored) {
                        return fileResource;
                    }
                }
                // 2. 匹配目录下的 index.html（如 html_2/ -> html_2/index.html）
                Resource indexResource = location.createRelative(normalized + "/index.html");
                if (indexResource.exists() && indexResource.isReadable()) {
                    return indexResource;
                }
                return null;
            }
        };
    }
}
