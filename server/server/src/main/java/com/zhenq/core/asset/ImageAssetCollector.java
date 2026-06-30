package com.zhenq.core.asset;

import cn.hutool.core.util.StrUtil;
import com.zhenq.config.AppProperties;
import com.zhenq.core.asset.model.ImageAssetBundle;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 图片素材收集器：并发调用 Pexels 与 unDraw
 */
@Slf4j
@Service
public class ImageAssetCollector {

    @Resource
    private AppProperties appProperties;

    @Resource
    private PexelsClient pexelsClient;

    @Resource
    private UndrawClient undrawClient;

    /**
     * 根据用户描述并发搜集图片素材
     */
    public ImageAssetBundle collect(String userMessage) {
        if (!appProperties.isAssetCollectionEnabled()) {
            return ImageAssetBundle.builder().keyword("").build();
        }
        String keyword = extractKeyword(userMessage);
        CompletableFuture<ImageAssetBundle> pexelsFuture = CompletableFuture.supplyAsync(() ->
                ImageAssetBundle.builder()
                        .keyword(keyword)
                        .photos(pexelsClient.search(keyword, appProperties.getPexelsPerPage()))
                        .build());
        CompletableFuture<ImageAssetBundle> undrawFuture = CompletableFuture.supplyAsync(() ->
                ImageAssetBundle.builder()
                        .keyword(keyword)
                        .illustrations(undrawClient.search(keyword, appProperties.getUndrawPerPage()))
                        .build());
        try {
            ImageAssetBundle fromPexels = pexelsFuture.join();
            ImageAssetBundle fromUndraw = undrawFuture.join();
            ImageAssetBundle bundle = ImageAssetBundle.builder()
                    .keyword(keyword)
                    .photos(fromPexels.getPhotos())
                    .illustrations(fromUndraw.getIllustrations())
                    .build();
            log.info("图片素材收集完成：{}", bundle.summary());
            return bundle;
        } catch (Exception e) {
            log.warn("图片素材收集异常: {}", e.getMessage());
            return ImageAssetBundle.builder().keyword(keyword).build();
        }
    }

    /**
     * 从用户描述提取搜索关键词（优先英文词，否则使用默认词）
     */
    String extractKeyword(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return "website";
        }
        String normalized = userMessage.replaceAll("[\\p{Punct}&&[^\\s]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            return "website";
        }
        // 优先提取英文单词作为 Pexels/unDraw 搜索词
        String[] tokens = normalized.split("\\s+");
        StringBuilder english = new StringBuilder();
        for (String token : tokens) {
            if (token.matches("[A-Za-z][A-Za-z0-9-]{1,30}")) {
                if (!english.isEmpty()) {
                    english.append(' ');
                }
                english.append(token.toLowerCase());
                if (english.length() > 40) {
                    break;
                }
            }
        }
        if (!english.isEmpty()) {
            return english.toString();
        }
        // 中文场景：使用常见主题映射
        if (normalized.contains("电商") || normalized.contains("商城") || normalized.contains("购物")) {
            return "shopping";
        }
        if (normalized.contains("教育") || normalized.contains("学习") || normalized.contains("课程")) {
            return "education";
        }
        if (normalized.contains("旅游") || normalized.contains("旅行")) {
            return "travel";
        }
        if (normalized.contains("美食") || normalized.contains("餐厅")) {
            return "food";
        }
        if (normalized.contains("科技") || normalized.contains("数据")) {
            return "technology";
        }
        return "website";
    }
}
