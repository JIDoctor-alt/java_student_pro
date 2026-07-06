-- 对话历史表
-- 数据库：student_pro

use student_pro;

create table if not exists `chat_history`
(
    id           bigint auto_increment primary key comment 'id',
    app_id       bigint                             not null comment '应用 id',
    message_type varchar(32)                        not null comment '消息类型：user / ai / error',
    content      text                               not null comment '消息内容',
    user_id      bigint                             null comment '发送用户 id（用户消息时有值）',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除：0 未删除，1 已删除',
    index idx_app_id_id (app_id, id),
    index idx_app_id_create_time (app_id, create_time)
) comment '应用对话历史' collate = utf8mb4_unicode_ci;
