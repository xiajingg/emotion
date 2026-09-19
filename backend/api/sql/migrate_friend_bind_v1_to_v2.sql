-- ========================================
-- 好友绑定表结构迁移脚本（v1 -> v2）
-- 从双向记录模型迁移到共享码中心模型
-- ========================================

-- 1. 备份旧表数据
CREATE TABLE IF NOT EXISTS `friend_bind_backup` AS SELECT * FROM `friend_bind`;

-- 2. 删除旧表
DROP TABLE IF EXISTS `friend_bind`;

-- 3. 创建新表结构
CREATE TABLE `friend_bind` (
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20) NOT NULL COMMENT '用户ID',
    `bind_code`       varchar(32) NOT NULL COMMENT '共享码（群组标识）',
    `status`          tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-待绑定/初始化, 1-已绑定',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_code` (`user_id`, `bind_code`) COMMENT '确保同一用户在同一群组中只有一条记录',
    KEY `idx_bind_code` (`bind_code`) COMMENT '加速通过共享码查找群组成员',
    KEY `idx_user_id` (`user_id`) COMMENT '加速通过用户ID查找绑定记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友绑定关系表（共享码中心模型）';

-- 4. 数据迁移逻辑（需要根据实际情况调整）
-- 注意：这个迁移脚本假设旧表中存在双向记录（A->B 和 B->A）
-- 如果旧表中有自引用记录（userId == friendUserId），需要特殊处理

INSERT INTO `friend_bind` (`user_id`, `bind_code`, `status`, `create_time`, `update_time`)
SELECT DISTINCT 
    user_id,
    bind_code,
    1 as status, -- 所有已存在的记录都标记为已绑定
    create_time,
    update_time
FROM `friend_bind_backup`
WHERE status = 1
GROUP BY user_id, bind_code;

-- 5. 验证迁移结果
SELECT COUNT(*) as '迁移后的记录数' FROM `friend_bind`;
SELECT COUNT(*) as '备份的记录数' FROM `friend_bind_backup`;

-- 6. 如果验证通过，可以删除备份表
-- DROP TABLE IF EXISTS `friend_bind_backup`;
