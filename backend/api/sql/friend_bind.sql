-- 好友绑定关系表
CREATE TABLE `friend_bind` (
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20) NOT NULL COMMENT '用户ID',
    `friend_user_id`  bigint(20) NOT NULL COMMENT '好友用户ID',
    `bind_code`       varchar(32) NOT NULL COMMENT '共享码（由发起方生成）',
    `status`          tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-已绑定, 0-已解绑',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`) COMMENT '确保两人之间只有一条绑定记录',
    KEY `idx_bind_code` (`bind_code`) COMMENT '加速通过共享码查找绑定关系',
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友双向绑定关系表';
