CREATE TABLE `user_text_interactions`
(
    `id`          int(11) NOT NULL AUTO_INCREMENT,
    `user_id`     varchar(32) NOT NULL,
    `input_text`  mediumtext  NOT NULL,
    `ai_response` text,
    `longitude`   decimal(9, 6)        DEFAULT NULL,
    `latitude`    decimal(9, 6)        DEFAULT NULL,
    `create_time` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `wechat_user`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `open_id`     varchar(255) DEFAULT NULL,
    `session_key` varchar(255) DEFAULT NULL,
    `token`       varchar(255) DEFAULT NULL,
    `valid_time`  varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `reward_record`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id`     bigint(20) NOT NULL,
    `param`       varchar(255) DEFAULT NULL,
    `create_time` datetime     DEFAULT NULL,
    `count`       int(11)      DEFAULT '1',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励记录表';