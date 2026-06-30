package com.zhenq.core.asset.model;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片素材汇总（Pexels 照片 + unDraw 插画）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssetBundle {

    private String keyword;

    @Builder.Default
    private List<PhotoAsset> photos = new ArrayList<>();

    @Builder.Default
    private List<IllustrationAsset> illustrations = new ArrayList<>();

    public boolean isEmpty() {
        return CollUtil.isEmpty(photos) && CollUtil.isEmpty(illustrations);
    }

    public String summary() {
        return String.format("keyword=%s, photos=%d, illustrations=%d",
                keyword, photos.size(), illustrations.size());
    }

    /**
     * 格式化为可注入 Prompt 的素材说明块
     */
    public String toPromptBlock() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【图片素材】系统已为你搜索到以下可用素材，请在页面中合理使用（仅可使用下列 URL，勿引入其他外链）：\n");
        if (CollUtil.isNotEmpty(photos)) {
            sb.append("\n## Pexels 照片\n");
            for (int i = 0; i < photos.size(); i++) {
                PhotoAsset photo = photos.get(i);
                sb.append(i + 1).append(". ")
                        .append(photo.getAlt() != null ? photo.getAlt() : "photo")
                        .append(" — ").append(photo.getUrl());
                if (photo.getPhotographer() != null) {
                    sb.append(" (by ").append(photo.getPhotographer()).append(")");
                }
                sb.append("\n");
            }
        }
        if (CollUtil.isNotEmpty(illustrations)) {
            sb.append("\n## unDraw 插画\n");
            for (int i = 0; i < illustrations.size(); i++) {
                IllustrationAsset ill = illustrations.get(i);
                sb.append(i + 1).append(". ")
                        .append(ill.getTitle() != null ? ill.getTitle() : "illustration")
                        .append(" — 页面: ").append(ill.getPageUrl());
                if (ill.getSvgUrl() != null) {
                    sb.append("，SVG: ").append(ill.getSvgUrl());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
