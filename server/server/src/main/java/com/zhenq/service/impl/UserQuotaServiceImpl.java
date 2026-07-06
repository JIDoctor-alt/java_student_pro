package com.zhenq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhenq.common.ErrorCode;
import com.zhenq.config.AppProperties;
import com.zhenq.exception.BusinessException;
import com.zhenq.exception.ThrowUtils;
import com.zhenq.mapper.UserMapper;
import com.zhenq.model.entity.User;
import com.zhenq.model.enums.ChatMessageTypeEnum;
import com.zhenq.model.enums.UserPlanEnum;
import com.zhenq.model.vo.QuotaPlanVO;
import com.zhenq.model.vo.UserQuotaVO;
import com.zhenq.service.AppService;
import com.zhenq.service.ChatHistoryService;
import com.zhenq.service.UserQuotaService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户额度服务：套餐分级 + 付费扩容 + Redis 计次
 */
@Service
public class UserQuotaServiceImpl implements UserQuotaService {

    private static final String LOGIN_BONUS_KEY_PREFIX = "quota:login_bonus:";
    private static final String CHAT_USED_KEY_PREFIX = "quota:chat_used:";

    @Resource
    private AppProperties appProperties;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserQuotaVO getUserQuota(User user) {
        UserQuotaVO vo = new UserQuotaVO();
        if (user == null) {
            return vo;
        }
        fillExpandMeta(vo);
        vo.setUpgradeEnabled(false);
        vo.setExpandEnabled(false);

        User fresh = userMapper.selectOneById(user.getId());
        if (fresh == null) {
            return vo;
        }
        return buildQuotaVo(fresh, vo);
    }

    private UserQuotaVO buildQuotaVo(User fresh, UserQuotaVO vo) {
        UserPlanEnum planEnum = UserPlanEnum.getEnumByValue(fresh.getUserPlan());
        AppProperties.PlanConfig planConfig = getPlanConfig(planEnum.getValue());

        int extraChat = safeInt(fresh.getExtraChatQuota());
        int extraApp = safeInt(fresh.getExtraAppQuota());
        boolean bonusGranted = hasDailyLoginBonus(fresh.getId());

        long planChatLimit = planConfig.getDailyChatLimit();
        long planAppLimit = planConfig.getMaxAppCount();
        long chatLimit = planChatLimit + extraChat + (bonusGranted ? appProperties.getQuota().getDailyLoginBonus() : 0);

        vo.setUserPlan(planEnum.getValue());
        vo.setUserPlanLabel(planConfig.getLabel());
        vo.setPlanChatLimit(planChatLimit);
        vo.setPlanAppLimit(planAppLimit);
        vo.setExtraChatQuota(extraChat);
        vo.setExtraAppQuota(extraApp);
        vo.setDailyLoginBonus(appProperties.getQuota().getDailyLoginBonus());
        vo.setDailyBonusGranted(bonusGranted);
        vo.setChatUsed(getChatUsedToday(fresh.getId()));
        vo.setChatLimit(chatLimit);
        vo.setAppUsed(countUserApps(fresh.getId()));
        vo.setAppLimit(planAppLimit + extraApp);
        vo.setUnlimited(false);
        vo.setUpgradeEnabled(false);
        vo.setPlans(listPlans(fresh));
        return vo;
    }

    @Override
    public List<QuotaPlanVO> listPlans(User user) {
        List<QuotaPlanVO> list = new ArrayList<>();
        String currentPlan = user == null ? UserPlanEnum.FREE.getValue()
                : UserPlanEnum.getEnumByValue(user.getUserPlan()).getValue();
        for (Map.Entry<String, AppProperties.PlanConfig> entry : appProperties.getQuota().getPlans().entrySet()) {
            AppProperties.PlanConfig cfg = entry.getValue();
            QuotaPlanVO item = new QuotaPlanVO();
            item.setPlan(entry.getKey());
            item.setLabel(cfg.getLabel());
            item.setDailyChatLimit(cfg.getDailyChatLimit());
            item.setMaxAppCount(cfg.getMaxAppCount());
            item.setPriceYuan(cfg.getPriceYuan());
            item.setCurrent(entry.getKey().equalsIgnoreCase(currentPlan));
            list.add(item);
        }
        return list;
    }

    @Override
    public void grantDailyLoginBonus(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        String key = loginBonusKey(userId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, "1", secondsUntilTomorrow(), TimeUnit.SECONDS);
    }

    @Override
    public void checkCanCreateApp(User user) {
        if (user == null) {
            return;
        }
        UserQuotaVO quota = getUserQuota(user);
        if (quota.getAppUsed() >= quota.getAppLimit()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                    "作品数量已达上限（" + quota.getAppUsed() + "/" + quota.getAppLimit() + "），请扩容或删除旧作品");
        }
    }

    @Override
    public void checkCanChat(User user) {
        if (user == null) {
            return;
        }
        UserQuotaVO quota = getUserQuota(user);
        if (quota.getChatUsed() >= quota.getChatLimit()) {
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED,
                    "今日对话次数已用完（" + quota.getChatUsed() + "/" + quota.getChatLimit() + "），请扩容或明日再试");
        }
    }

    @Override
    public void recordChatUsage(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        String key = chatUsedKey(userId);
        Long value = stringRedisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            stringRedisTemplate.expire(key, secondsUntilTomorrow(), TimeUnit.SECONDS);
        }
    }

    @Override
    public UserQuotaVO expandQuota(User user, String type, int packs) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "付费扩容功能暂未开发");
    }

    @Override
    public UserQuotaVO upgradePlan(User user, String targetPlan) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "升级套餐功能暂未开发");
    }

    private void fillExpandMeta(UserQuotaVO vo) {
        AppProperties.QuotaProperties quota = appProperties.getQuota();
        vo.setChatExpandPack(quota.getChatExpandPack());
        vo.setAppExpandPack(quota.getAppExpandPack());
        vo.setChatExpandPriceYuan(quota.getChatExpandPriceYuan());
        vo.setAppExpandPriceYuan(quota.getAppExpandPriceYuan());
    }

    private AppProperties.PlanConfig getPlanConfig(String plan) {
        AppProperties.PlanConfig config = appProperties.getQuota().getPlans().get(plan);
        if (config == null) {
            config = appProperties.getQuota().getPlans().get(UserPlanEnum.FREE.getValue());
        }
        if (config == null) {
            config = new AppProperties.PlanConfig();
        }
        return config;
    }

    private long getChatUsedToday(Long userId) {
        String key = chatUsedKey(userId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        long dbCount = countTodayUserMessages(userId);
        if (dbCount > 0) {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(dbCount), secondsUntilTomorrow(), TimeUnit.SECONDS);
        }
        return dbCount;
    }

    private long countTodayUserMessages(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .eq("message_type", ChatMessageTypeEnum.USER.getValue())
                .ge("create_time", start)
                .lt("create_time", end);
        return chatHistoryService.count(queryWrapper);
    }

    private long countUserApps(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("user_id", userId);
        return appService.count(queryWrapper);
    }

    private boolean hasDailyLoginBonus(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(loginBonusKey(userId)));
    }

    private String loginBonusKey(Long userId) {
        return LOGIN_BONUS_KEY_PREFIX + userId + ":" + today();
    }

    private String chatUsedKey(Long userId) {
        return CHAT_USED_KEY_PREFIX + userId + ":" + today();
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private long secondsUntilTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(LocalTime.MIN);
        return Math.max(60, java.time.Duration.between(now, tomorrow).getSeconds());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
