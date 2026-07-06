package com.zhenq.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图（脱敏，用于展示给前台）
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user / admin
     */
    private String userRole;

    /**
     * 套餐
     */
    private String userPlan;

    /**
     * 付费扩容-对话
     */
    private Integer extraChatQuota;

    /**
     * 付费扩容-作品
     */
    private Integer extraAppQuota;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
