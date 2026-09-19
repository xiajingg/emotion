-- 答案之书预设答案库表
CREATE TABLE `answer_book_answers` (
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `content`     text NOT NULL COMMENT '预设的答案内容',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答案之书预设答案库';

-- 答案之书记录表
CREATE TABLE `answer_book_records` (
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint(20) NOT NULL COMMENT '用户ID',
    `question`        text NOT NULL COMMENT '用户提问',
    `random_answer`   text NOT NULL COMMENT '随机抽取的预设答案',
    `ai_explanation`  text NOT NULL COMMENT 'AI生成的个性化解读',
    `usage_count`     int(11) NOT NULL DEFAULT 1 COMMENT '消耗次数',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答案之书记录表';
