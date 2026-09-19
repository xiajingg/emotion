-- 优化 user_notification 表结构
-- 目标：每个用户每种类型只保留一条记录，每天更新而非插入新记录

-- 1. 添加唯一索引（确保每个用户每种类型只有一条记录）
ALTER TABLE `user_notification` 
ADD UNIQUE INDEX `uk_user_type` (`user_id`, `notification_type`) 
COMMENT '唯一索引：每个用户每种通知类型只保留一条记录';

-- 2. 清理历史重复数据（保留最新的记录）
-- 注意：这个操作会删除旧日期的重复记录，只保留每个用户每种类型的最新记录
DELETE n1 FROM user_notification n1
INNER JOIN user_notification n2 
WHERE n1.user_id = n2.user_id 
  AND n1.notification_type = n2.notification_type
  AND n1.id < n2.id;

-- 3. 优化说明
-- - 之前：每天为每个用户创建新记录，导致数据快速增长
-- - 现在：每个用户每种类型只保留一条记录，定时任务更新 trigger_date 和 is_read
-- - 好处：减少数据量，提高查询性能，避免冗余
