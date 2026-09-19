-- 情绪急救产品数据闭环：事件埋点与回复收藏
CREATE TABLE IF NOT EXISTS `emotion_event_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `interaction_id` BIGINT DEFAULT NULL COMMENT '关联 user_text_interactions.id',
  `event_type` VARCHAR(64) NOT NULL COMMENT 'submit_start/submit_success/reply_copy/favorite_create/rescue_action_done/template_share/weekly_report_open/subscribe_accept',
  `task_type` VARCHAR(32) DEFAULT NULL COMMENT 'REPLY_RESCUE/SLEEP_RUMINATION/PRESSURE_RESCUE/GENERAL',
  `scenario_key` VARCHAR(64) DEFAULT NULL COMMENT '细分场景',
  `reply_style` VARCHAR(32) DEFAULT NULL COMMENT 'safe/firm/gentle/short',
  `extra` VARCHAR(1024) DEFAULT NULL COMMENT '额外上下文，避免存原始隐私文本',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_event_time` (`event_type`, `create_time`),
  KEY `idx_interaction` (`interaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情绪急救事件埋点表';

CREATE TABLE IF NOT EXISTS `emotion_reply_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `interaction_id` BIGINT DEFAULT NULL COMMENT '关联 user_text_interactions.id',
  `scenario_key` VARCHAR(64) DEFAULT NULL COMMENT '细分场景',
  `reply_style` VARCHAR(32) DEFAULT NULL COMMENT 'safe/firm/gentle/short',
  `reply_text` VARCHAR(512) NOT NULL COMMENT '收藏的话术',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_interaction` (`interaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情绪急救回复收藏表';
