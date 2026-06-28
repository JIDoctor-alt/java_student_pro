-- 应用模块建表脚本
-- 数据库：student_pro

use student_pro;

-- 应用表
create table if not exists `app`
(
    id            bigint auto_increment primary key comment 'id',
    app_name      varchar(256)                       null comment '应用名称',
    cover         varchar(512)                       null comment '应用封面',
    init_prompt   text                               null comment '应用初始化的 prompt',
    code_gen_type varchar(64)                        null comment '代码生成类型：html / multi_file',
    deploy_key    varchar(64)                        null comment '部署标识',
    deployed_time datetime                           null comment '部署时间',
    priority      int          default 0             not null comment '优先级（精选为 99）',
    user_id       bigint                             not null comment '创建用户 id',
    edit_time     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0             not null comment '是否删除：0 未删除，1 已删除',
    index idx_app_name (app_name),
    index idx_user_id (user_id),
    index idx_deploy_key (deploy_key)
) comment '应用' collate = utf8mb4_unicode_ci;
