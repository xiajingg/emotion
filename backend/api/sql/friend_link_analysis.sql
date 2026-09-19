CREATE TABLE IF NOT EXISTS `friend_link_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bind_code` varchar(32) NOT NULL COMMENT '好友绑定共享码',
  `analysis_date` date NOT NULL COMMENT '分析日期',
  `title` varchar(64) NOT NULL DEFAULT '今日共鸣分析' COMMENT 'AI分析标题',
  `summary` varchar(512) NOT NULL COMMENT 'AI共鸣总结',
  `suggestion` varchar(512) DEFAULT NULL COMMENT '轻量行动建议',
  `answer_book` varchar(64) DEFAULT NULL COMMENT '答案之书',
  `me_count` int NOT NULL DEFAULT 0 COMMENT '当前视角我的记录数',
  `friend_count` int NOT NULL DEFAULT 0 COMMENT '当前视角好友记录数',
  `same_day_count` int NOT NULL DEFAULT 0 COMMENT '双方同天记录数',
  `ai_response` text COMMENT '原始AI响应JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind_code_analysis_date` (`bind_code`, `analysis_date`),
  KEY `idx_analysis_date` (`analysis_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心灵链接每日AI共鸣分析';
