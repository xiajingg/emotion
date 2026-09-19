-- 好友绑定关系表（共享码中心模型）
CREATE TABLE `friend_bind` (
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20) NOT NULL COMMENT '用户ID',
    `bind_code`       varchar(32) NOT NULL COMMENT '共享码（群组标识）',
    `status`          tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-待绑定/初始化, 1-已绑定',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`) COMMENT '确保每个用户只有一条记录，防止并发重复',
    KEY `idx_bind_code` (`bind_code`) COMMENT '加速通过共享码查找群组成员'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友绑定关系表（共享码中心模型）';
