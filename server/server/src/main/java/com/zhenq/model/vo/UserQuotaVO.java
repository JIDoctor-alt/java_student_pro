package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户额度视图（对话次数 / 作品数量 / 套餐）
 */
@Data
public class UserQuotaVO implements Serializable {

    /** 当前套餐 */
    private String userPlan;

    /** 套餐名称 */
    private String userPlanLabel;

    /** 今日已用对话次数（含 AI 对话 + 提示词优化） */
    private long chatUsed;

    /** 今日对话次数上限 */
    private long chatLimit;

    /** 套餐基础对话额度 */
    private long planChatLimit;

    /** 付费扩容对话额度 */
    private int extraChatQuota;

    /** 每日登录奖励 */
    private int dailyLoginBonus;

    /** 今日是否已领取登录奖励 */
    private boolean dailyBonusGranted;

    /** 已创建作品数 */
    private long appUsed;

    /** 作品数量上限 */
    private long appLimit;

    /** 套餐基础作品额度 */
    private long planAppLimit;

    /** 付费扩容作品额度 */
    private int extraAppQuota;

    /** 管理员不受限 */
    private boolean unlimited;

    /** 可选套餐列表 */
    private List<QuotaPlanVO> plans;

    /** 对话扩容包规格 */
    private int chatExpandPack;

    /** 作品扩容包规格 */
    private int appExpandPack;

    /** 对话扩容包单价（元） */
    private int chatExpandPriceYuan;

    /** 作品扩容包单价（元） */
    private int appExpandPriceYuan;

    /** 是否开放自助升级套餐 */
    private boolean upgradeEnabled;

    /** 是否开放自助付费扩容 */
    private boolean expandEnabled;
}
