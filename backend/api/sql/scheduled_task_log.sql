-- 定时任务执行记录表
CREATE TABLE `scheduled_task_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` VARCHAR(64) NOT NULL COMMENT '任务名称',
  `task_key` VARCHAR(128) NOT NULL COMMENT '任务唯一标识(用于分布式锁)',
  `execute_date` DATE NOT NULL COMMENT '执行日期',
  `execute_time` DATETIME NOT NULL COMMENT '执行时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态: 0-执行中, 1-成功, 2-失败',
  `instance_id` VARCHAR(64) DEFAULT NULL COMMENT '执行实例ID(服务器IP或容器ID)',
  `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_msg` TEXT COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_key_date` (`task_key`, `execute_date`) COMMENT '唯一索引: 同一天同一任务只执行一次',
  KEY `idx_execute_time` (`execute_time`) COMMENT '按执行时间查询',
  KEY `idx_status` (`status`) COMMENT '按状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行记录表';
