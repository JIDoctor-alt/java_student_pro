-- 用户套餐与付费扩容字段（已有库执行一次即可）
use student_pro;

alter table `user`
    add column user_plan varchar(32) default 'free' not null comment '套餐：free/basic/pro' after user_role;

alter table `user`
    add column extra_chat_quota int default 0 not null comment '付费扩容-额外每日对话次数' after user_plan;

alter table `user`
    add column extra_app_quota int default 0 not null comment '付费扩容-额外作品数量' after extra_chat_quota;
