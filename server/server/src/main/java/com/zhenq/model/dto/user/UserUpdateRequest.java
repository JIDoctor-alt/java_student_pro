package com.zhenq.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求（管理员）
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

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
     * 套餐：free / basic / pro
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
}
