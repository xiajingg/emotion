-- 每日星座运势表
CREATE TABLE IF NOT EXISTS `daily_horoscope` (
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `date`             date NOT NULL COMMENT '日期',
    `zodiac_sign`      varchar(20) NOT NULL COMMENT '星座名称 (如: Aries, Taurus)',
    `score`            int(11) DEFAULT NULL COMMENT '综合运势分数 (1-100)',
    `content`          text COMMENT 'AI生成的运势内容',
    `astro_analysis`   text COMMENT '天体专业分析',
    `love_fortune`     int(11) DEFAULT NULL COMMENT '情感运势分数 (1-100)',
    `wealth_fortune`   int(11) DEFAULT NULL COMMENT '财富运势分数 (1-100)',
    `career_fortune`   int(11) DEFAULT NULL COMMENT '事业运势分数 (1-100)',
    `dos_and_donts`    text COMMENT '宜忌事项',
    `ai_advice`        text COMMENT 'AI 综合建议',
    `ai_model`         varchar(50) DEFAULT NULL COMMENT '使用的AI模型',
    `create_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_sign` (`date`, `zodiac_sign`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日星座运势表';
