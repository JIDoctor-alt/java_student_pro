-- 用户模块建表脚本
-- 数据库：student_pro
-- 说明：列名采用下划线风格（MyBatis Flex 默认驼峰<->下划线映射），
--      实体 Java 字段为驼峰（userAccount 等），由 ORM 自动映射。

create database if not exists student_pro;

use student_pro;

-- 用户表
create table if not exists `user`
(
    id            bigint auto_increment primary key comment 'id',
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    user_plan     varchar(32)  default 'free'             not null comment '套餐：free/basic/pro',
    extra_chat_quota int       default 0                  not null comment '付费扩容-额外每日对话次数',
    extra_app_quota  int       default 0                  not null comment '付费扩容-额外作品数量',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除：0 未删除，1 已删除',
    index idx_user_account (user_account)
) comment '用户' collate = utf8mb4_unicode_ci;
