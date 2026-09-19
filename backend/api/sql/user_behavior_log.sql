-- 用户行为分析表
CREATE TABLE `user_behavior_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `behavior_type` TINYINT NOT NULL COMMENT '行为类型: 1-查看每日补给, 2-查看星座运势, 3-点击探索菜单',
  `notification_type` TINYINT DEFAULT NULL COMMENT '关联的提醒类型: 1-每日补给, 2-星座运势',
  `has_unread` TINYINT DEFAULT 0 COMMENT '当时是否有未读消息: 0-无, 1-有',
  `action_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
  `device_info` VARCHAR(256) DEFAULT NULL COMMENT '设备信息(可选)',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址(可选)',
  
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `action_time`) COMMENT '按用户和时间查询',
  KEY `idx_behavior_type` (`behavior_type`) COMMENT '按行为类型统计',
  KEY `idx_action_time` (`action_time`) COMMENT '按时间范围查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为分析表';
