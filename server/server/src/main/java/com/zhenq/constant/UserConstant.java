package com.zhenq.constant;

/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键（存储在 Session 中）
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 普通用户角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 盐值，混淆密码
     */
    String SALT = "zhenq";
}
