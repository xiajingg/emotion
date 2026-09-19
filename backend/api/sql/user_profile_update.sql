-- 用户资料字段更新脚本
-- 为 wechat_user 表添加昵称和星座字段

ALTER TABLE `wechat_user` 
ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称' AFTER `open_id`,
ADD COLUMN `constellation` VARCHAR(20) DEFAULT NULL COMMENT '星座' AFTER `nickname`;
