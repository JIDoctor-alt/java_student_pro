package com.zhenq.core.asset;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhenq.config.AppProperties;
import com.zhenq.core.asset.model.PhotoAsset;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片搜索客户端
 * <p>
 * API 文档：https://www.pexels.com/api/documentation/
 */
@Slf4j
@Component
public class PexelsClient {

    private static final String SEARCH_URL = "https://api.pexels.com/v1/search";

    @Resource
    private AppProperties appProperties;

    public List<PhotoAsset> search(String keyword, int perPage) {
        List<PhotoAsset> result = new ArrayList<>();
        if (StrUtil.isBlank(appProperties.getPexelsApiKey())) {
            log.debug("Pexels API Key 未配置，跳过照片搜索");
            return result;
        }
        if (StrUtil.isBlank(keyword)) {
            return result;
        }
        int limit = Math.max(1, Math.min(perPage, 10));
        try {
            HttpResponse response = HttpRequest.get(SEARCH_URL)
                    .header("Authorization", appProperties.getPexelsApiKey())
                    .form("query", keyword.trim())
                    .form("per_page", limit)
                    .form("locale", "zh-CN")
                    .timeout(8000)
                    .execute();
            if (!response.isOk()) {
                log.warn("Pexels 搜索失败，HTTP {}: {}", response.getStatus(), response.body());
                return result;
            }
            JSONObject root = JSONUtil.parseObj(response.body());
            JSONArray photos = root.getJSONArray("photos");
            if (photos == null) {
                return result;
            }
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                JSONObject src = photo.getJSONObject("src");
                if (src == null) {
                    continue;
                }
                String url = StrUtil.blankToDefault(src.getStr("large"), src.getStr("medium"));
                if (StrUtil.isBlank(url)) {
                    continue;
                }
                result.add(PhotoAsset.builder()
                        .url(url)
                        .alt(photo.getStr("alt"))
                        .photographer(photo.getStr("photographer"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Pexels 搜索异常 keyword={}: {}", keyword, e.getMessage());
        }
        return result;
    }
}
