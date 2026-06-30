package com.zhenq.core.cover;

import com.zhenq.config.AppProperties;
import com.zhenq.constant.AppConstant;
import com.zhenq.core.vue.VueProjectPreviewUtils;
import com.zhenq.model.entity.App;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 使用 Selenium + WebDriverManager 对应用预览页截图，生成封面图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppCoverScreenshotService {

    private final AppProperties appProperties;

    /**
     * 对应用预览页截图并保存到本地封面目录
     *
     * @return 封面文件；失败返回 null
     */
    public File capturePreviewScreenshot(App app) {
        if (app == null || app.getId() == null) {
            return null;
        }
        if (!appProperties.isCoverEnabled()) {
            log.debug("封面生成已关闭，跳过 appId={}", app.getId());
            return null;
        }
        String codeGenType = app.getCodeGenType();
        if (!VueProjectPreviewUtils.isPreviewReady(codeGenType, app.getId())) {
            log.warn("预览页尚未就绪，跳过封面截图 appId={}", app.getId());
            return null;
        }

        String previewUrl = AppPreviewUrlUtils.buildPreviewPageUrl(
                appProperties.getServerBaseUrl(), codeGenType, app.getId());
        File coverDir = new File(AppConstant.CODE_COVER_ROOT_DIR);
        if (!coverDir.exists() && !coverDir.mkdirs()) {
            log.warn("无法创建封面目录: {}", coverDir.getAbsolutePath());
            return null;
        }
        File coverFile = new File(coverDir, AppPreviewUrlUtils.buildCoverFileName(app.getId()));

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=" + appProperties.getCoverWidth() + ","
                + appProperties.getCoverHeight());

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(
                    appProperties.getCoverWidth(), appProperties.getCoverHeight()));
            log.info("开始截图封面 appId={} url={}", app.getId(), previewUrl);
            driver.get(previewUrl);
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> "complete".equals(
                            ((JavascriptExecutor) d).executeScript("return document.readyState")));
            int waitSeconds = Math.max(0, appProperties.getCoverWaitSeconds());
            if (waitSeconds > 0) {
                Thread.sleep(waitSeconds * 1000L);
            }
            File tempShot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(tempShot.toPath(), coverFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("封面截图成功 appId={} file={}", app.getId(), coverFile.getAbsolutePath());
            return coverFile;
        } catch (Exception e) {
            log.warn("封面截图失败 appId={}: {}", app.getId(), e.getMessage());
            return null;
        } finally {
            driver.quit();
        }
    }
}
