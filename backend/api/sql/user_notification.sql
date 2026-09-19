-- 用户消息提醒表
CREATE TABLE `user_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `notification_type` TINYINT NOT NULL COMMENT '提醒类型: 1-每日补给, 2-星座运势',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type_date` (`user_id`, `notification_type`) COMMENT '唯一索引: 每个用户每种通知类型只有一条记录',
  KEY `idx_user_unread` (`user_id`, `is_read`) COMMENT '查询未读消息的索引',
  KEY `idx_create_time` (`create_time`) COMMENT '按创建时间查询的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息提醒表';
