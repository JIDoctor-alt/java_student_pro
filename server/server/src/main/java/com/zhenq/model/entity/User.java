package com.zhenq.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Table("user")
public class User implements Serializable {

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

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
     * 付费扩容：额外每日对话次数
     */
    private Integer extraChatQuota;

    /**
     * 付费扩容：额外作品数量
     */
    private Integer extraAppQuota;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0 未删除，1 已删除（逻辑删除）
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}
