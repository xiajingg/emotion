-- 修复并发问题：为 user_id 添加唯一索引
-- 确保每个用户只能有一条记录（无论 status 是什么）

-- 1. 先清理重复数据（保留最新的记录）
DELETE t1 FROM friend_bind t1
INNER JOIN friend_bind t2 
WHERE t1.id < t2.id 
  AND t1.user_id = t2.user_id;

-- 2. 删除旧的组合唯一索引
ALTER TABLE friend_bind DROP INDEX uk_user_code;

-- 3. 添加 user_id 的唯一索引
ALTER TABLE friend_bind ADD UNIQUE KEY uk_user_id (user_id);

-- 4. 保留其他索引
-- idx_bind_code 和 idx_user_id 已经存在，无需修改
