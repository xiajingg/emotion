-- 用户周报订阅行为日志表（用户画像用）
-- 每条订阅授权、每次推送发送（无论成功失败）都独立记录一行，永不更新
CREATE TABLE IF NOT EXISTS `user_weekly_report_action_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `open_id` VARCHAR(128) DEFAULT NULL COMMENT '微信OpenID',
  `template_id` VARCHAR(128) DEFAULT NULL COMMENT '订阅消息模板ID',
  `action_type` VARCHAR(32) NOT NULL COMMENT '行为类型: SUBSCRIBE-授权订阅, PUSH-推送发送',
  `action_time` DATETIME NOT NULL COMMENT '行为发生时间',
  `push_success` TINYINT DEFAULT NULL COMMENT '推送是否成功(仅PUSH类型): 1-成功, 0-失败',
  `push_err_code` VARCHAR(32) DEFAULT NULL COMMENT '推送失败错误码',
  `push_err_msg` VARCHAR(512) DEFAULT NULL COMMENT '推送失败错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_action_time` (`user_id`, `action_type`, `action_time`),
  KEY `idx_action_time` (`action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户周报订阅行为日志（用户画像）';
