-- 用户使用次数变动日志表
CREATE TABLE `user_usage_log` (
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20) NOT NULL COMMENT '用户ID',
    `operation_type`  varchar(50) NOT NULL COMMENT '操作类型：EMOTION_ANALYSIS-情绪分析, ANSWER_BOOK-答案之书, SIGN_IN-签到, AD_REWARD-广告奖励, PAYMENT-支付购买, DAILY_BONUS-每日名额',
    `change_amount`   int(11) NOT NULL COMMENT '变动数量（负数表示消耗，正数表示增加）',
    `balance_before`  int(11) NOT NULL COMMENT '变动前余额',
    `balance_after`   int(11) NOT NULL COMMENT '变动后余额',
    `related_id`      bigint(20) DEFAULT NULL COMMENT '关联记录ID（如答案之书记录ID、支付订单ID等）',
    `remark`          varchar(500) DEFAULT NULL COMMENT '备注说明',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户使用次数变动日志表';
