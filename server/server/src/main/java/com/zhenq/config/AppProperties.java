package com.zhenq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * 本服务对外访问前缀（预览、封面等静态资源 URL 拼接用）
     */
    private String serverBaseUrl = "http://127.0.0.1:8123/api";

    /**
     * 是否启用 Selenium 自动生成应用封面
     */
    private boolean coverEnabled = true;

    /**
     * 封面截图宽度（像素）
     */
    private int coverWidth = 1280;

    /**
     * 封面截图高度（像素）
     */
    private int coverHeight = 800;

    /**
     * 页面加载后额外等待秒数（等待渲染完成）
     */
    private int coverWaitSeconds = 3;

    /**
     * 是否启用 AI 工作流中的图片素材收集（Pexels + unDraw）
     */
    private boolean assetCollectionEnabled = true;

    /**
     * Pexels API Key（https://www.pexels.com/api/）
     */
    private String pexelsApiKey = "";

    /**
     * Pexels 每次搜索返回的图片数量
     */
    private int pexelsPerPage = 3;

    /**
     * unDraw 每次搜索返回的插画数量
     */
    private int undrawPerPage = 3;

    /**
     * 质量检测失败时的最大重试次数（0 表示不重试）
     */
    private int workflowMaxRetries = 1;

    /**
     * 是否启用 LangGraph4j Checkpoint 持久化（Redis）
     */
    private boolean workflowCheckpointEnabled = true;

    /**
     * Checkpoint 使用的 Redis database（建议与 Session db 分离）
     */
    private int workflowCheckpointRedisDatabase = 2;

    /**
     * 用户额度（控制 AI 成本，参考 nocode 对话次数 / 作品数量）
     */
    private QuotaProperties quota = new QuotaProperties();

    @Data
    public static class QuotaProperties {
        /**
         * 每日首次登录额外奖励
         */
        private int dailyLoginBonus = 10;
        /**
         * 单次购买对话扩容包大小
         */
        private int chatExpandPack = 50;
        /**
         * 单次购买作品扩容包大小
         */
        private int appExpandPack = 5;
        /**
         * 对话扩容包单价（元，模拟）
         */
        private int chatExpandPriceYuan = 9;
        /**
         * 作品扩容包单价（元，模拟）
         */
        private int appExpandPriceYuan = 19;
        /**
         * 套餐分级配置
         */
        private Map<String, PlanConfig> plans = defaultPlans();

        private static Map<String, PlanConfig> defaultPlans() {
            Map<String, PlanConfig> map = new LinkedHashMap<>();
            PlanConfig free = new PlanConfig();
            free.setLabel("免费版");
            free.setDailyChatLimit(50);
            free.setMaxAppCount(5);
            free.setPriceYuan(0);
            map.put("free", free);

            PlanConfig basic = new PlanConfig();
            basic.setLabel("基础版");
            basic.setDailyChatLimit(150);
            basic.setMaxAppCount(10);
            basic.setPriceYuan(29);
            map.put("basic", basic);

            PlanConfig pro = new PlanConfig();
            pro.setLabel("专业版");
            pro.setDailyChatLimit(320);
            pro.setMaxAppCount(20);
            pro.setPriceYuan(99);
            map.put("pro", pro);
            return map;
        }
    }

    @Data
    public static class PlanConfig {
        private String label = "";
        private int dailyChatLimit = 50;
        private int maxAppCount = 5;
        private int priceYuan = 0;
    }
}
