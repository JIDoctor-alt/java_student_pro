package com.zhenq.core.cover;

import com.zhenq.config.AppProperties;
import com.zhenq.manager.CosManager;
import com.zhenq.model.entity.App;
import com.zhenq.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

/**
 * 应用封面生成门面：异步截图并回写 app.cover
 */
@Slf4j
@Service
public class AppCoverService {

    @Resource
    private AppService appService;

    @Resource
    private AppCoverScreenshotService appCoverScreenshotService;

    @Resource
    private AppProperties appProperties;

    @Resource
    private CosManager cosManager;

    /**
     * 异步生成封面（生成完成后调用，不阻塞 SSE）
     */
    public void generateCoverAsync(Long appId) {
        generateCoverAsync(appId, null);
    }

    /**
     * 异步生成封面，完成后回调封面 URL（失败或未启用时为 null）
     */
    public void generateCoverAsync(Long appId, Consumer<String> onComplete) {
        if (appId == null || appId <= 0) {
            invokeCallback(onComplete, null);
            return;
        }
        if (!appProperties.isCoverEnabled()) {
            invokeCallback(onComplete, null);
            return;
        }
        Schedulers.boundedElastic().schedule(() -> {
            String coverUrl = null;
            try {
                coverUrl = generateCover(appId);
            } catch (Exception e) {
                log.warn("异步封面生成异常 appId={}: {}", appId, e.getMessage());
            }
            invokeCallback(onComplete, coverUrl);
        });
    }

    private void invokeCallback(Consumer<String> onComplete, String coverUrl) {
        if (onComplete != null) {
            onComplete.accept(coverUrl);
        }
    }

    /**
     * 同步生成封面并返回封面 URL
     */
    public String generateCover(Long appId) {
        App app = appService.getById(appId);
        if (app == null) {
            return null;
        }
        var coverFile = appCoverScreenshotService.capturePreviewScreenshot(app);
        if (coverFile == null || !coverFile.exists()) {
            return null;
        }
        String coverUrl = resolveCoverPublicUrl(appId, coverFile);
        App update = new App();
        update.setId(appId);
        update.setCover(coverUrl);
        appService.updateById(update);
        log.info("应用封面已更新 appId={} cover={}", appId, coverUrl);
        return coverUrl;
    }

    /**
     * 优先上传到 COS；未配置 COS 时回退到本机静态目录 URL
     */
    private String resolveCoverPublicUrl(Long appId, java.io.File coverFile) {
        if (cosManager.isEnabled()) {
            try {
                String objectKey = cosManager.buildObjectKey(
                        AppPreviewUrlUtils.buildCoverObjectKey(appId));
                cosManager.putObject(objectKey, coverFile);
                return cosManager.getObjectUrl(objectKey);
            } catch (Exception e) {
                log.warn("封面上传 COS 失败 appId={}，回退本地静态 URL: {}", appId, e.getMessage());
            }
        }
        return AppPreviewUrlUtils.buildCoverPublicUrl(appProperties.getServerBaseUrl(), appId);
    }
}
