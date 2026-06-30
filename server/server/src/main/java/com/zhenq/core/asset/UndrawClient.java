package com.zhenq.core.asset;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhenq.core.asset.model.IllustrationAsset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * unDraw 插画搜索客户端
 * <p>
 * 通过 Next.js data API 搜索插画，buildId 从首页动态获取以避免硬编码失效。
 */
@Slf4j
@Component
public class UndrawClient {

    private static final String HOME_URL = "https://undraw.co/";
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("\"buildId\"\\s*:\\s*\"([^\"]+)\"");

    private volatile String cachedBuildId;
    private volatile long buildIdFetchedAt;

    public List<IllustrationAsset> search(String keyword, int limit) {
        List<IllustrationAsset> result = new ArrayList<>();
        if (StrUtil.isBlank(keyword)) {
            return result;
        }
        String buildId = resolveBuildId();
        if (StrUtil.isBlank(buildId)) {
            log.warn("无法获取 unDraw buildId，跳过插画搜索");
            return result;
        }
        int max = Math.max(1, Math.min(limit, 10));
        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = String.format("https://undraw.co/_next/data/%s/search/%s.json?term=%s",
                    buildId, encoded, encoded);
            HttpResponse response = HttpRequest.get(url)
                    .header("Accept", "application/json")
                    .timeout(8000)
                    .execute();
            if (!response.isOk()) {
                log.warn("unDraw 搜索失败，HTTP {}: {}", response.getStatus(), response.body());
                return result;
            }
            JSONObject root = JSONUtil.parseObj(response.body());
            JSONArray initialResults = root.getByPath("pageProps.initialResults", JSONArray.class);
            if (initialResults == null) {
                return result;
            }
            for (int i = 0; i < initialResults.size() && result.size() < max; i++) {
                JSONObject node = initialResults.getJSONObject(i);
                String slug = node.getStr("newSlug");
                String title = node.getStr("title");
                if (StrUtil.isBlank(slug)) {
                    continue;
                }
                String pageUrl = "https://undraw.co/illustrations/" + slug;
                result.add(IllustrationAsset.builder()
                        .title(title)
                        .pageUrl(pageUrl)
                        .svgUrl(pageUrl + ".svg")
                        .build());
            }
        } catch (Exception e) {
            log.warn("unDraw 搜索异常 keyword={}: {}", keyword, e.getMessage());
        }
        return result;
    }

    private String resolveBuildId() {
        long now = System.currentTimeMillis();
        if (cachedBuildId != null && now - buildIdFetchedAt < 3600_000L) {
            return cachedBuildId;
        }
        synchronized (this) {
            if (cachedBuildId != null && now - buildIdFetchedAt < 3600_000L) {
                return cachedBuildId;
            }
            try {
                HttpResponse response = HttpRequest.get(HOME_URL).timeout(8000).execute();
                if (!response.isOk()) {
                    return cachedBuildId;
                }
                Matcher matcher = BUILD_ID_PATTERN.matcher(response.body());
                if (matcher.find()) {
                    cachedBuildId = matcher.group(1);
                    buildIdFetchedAt = System.currentTimeMillis();
                }
            } catch (Exception e) {
                log.warn("获取 unDraw buildId 失败: {}", e.getMessage());
            }
            return cachedBuildId;
        }
    }
}
