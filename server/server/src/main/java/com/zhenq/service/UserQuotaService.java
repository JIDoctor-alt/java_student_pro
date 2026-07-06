package com.zhenq.service;

import com.zhenq.model.entity.User;
import com.zhenq.model.vo.QuotaPlanVO;
import com.zhenq.model.vo.UserQuotaVO;

import java.util.List;

/**
 * 用户额度服务：套餐分级、付费扩容、AI 用量控制
 */
public interface UserQuotaService {

    UserQuotaVO getUserQuota(User user);

    List<QuotaPlanVO> listPlans(User user);

    void grantDailyLoginBonus(Long userId);

    void checkCanCreateApp(User user);

    void checkCanChat(User user);

    /** 记录一次 AI 对话或提示词优化消耗 */
    void recordChatUsage(Long userId);

    /** 模拟购买扩容包 */
    UserQuotaVO expandQuota(User user, String type, int packs);

    /** 模拟升级套餐 */
    UserQuotaVO upgradePlan(User user, String targetPlan);
}
