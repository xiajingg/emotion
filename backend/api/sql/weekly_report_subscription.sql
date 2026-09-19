-- 用户周报推送授权记录表
-- 微信订阅消息为一次性：用户每次弹窗授权后 status=1，
-- 推送成功或微信明确拒收后 status=0（已消耗），下次推送前需重新授权
CREATE TABLE IF NOT EXISTS `user_weekly_report_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `open_id` VARCHAR(100) NOT NULL COMMENT '微信OpenID',
  `template_id` VARCHAR(100) NOT NULL COMMENT '订阅消息模板ID',
  `subscribe_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间（用户弹窗点允许的时间）',
  `last_send_time` DATETIME DEFAULT NULL COMMENT '推送成功或微信明确拒收时间（授权消耗时间）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-已授权待推送，0-已推送/已消耗',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_template` (`user_id`, `template_id`),
  KEY `idx_open_id` (`open_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户周报推送授权记录';
