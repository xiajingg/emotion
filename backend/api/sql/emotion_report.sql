-- ============================================================
-- 情绪报告表
-- 定时任务每周一 0 点自动调用 AI 生成，前端直接从表里读取
-- report_period 支持 WEEK / MONTH，为后续月报扩展预留
-- ============================================================
DROP TABLE IF EXISTS `emotion_report`;
CREATE TABLE `emotion_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `report_period` VARCHAR(10) NOT NULL COMMENT '报告周期: WEEK / MONTH',
    `period_start` DATE NOT NULL COMMENT '周期开始日期',
    `period_end` DATE NOT NULL COMMENT '周期结束日期',
    `date_range` VARCHAR(50) DEFAULT NULL COMMENT '日期范围展示: 05-17至05-23',
    `record_count` INT DEFAULT 0 COMMENT '记录次数',
    `check_in_days` INT DEFAULT 0 COMMENT '打卡天数',
    `main_emotion` VARCHAR(50) DEFAULT NULL COMMENT '主打情绪标签',
    `emotion_state` VARCHAR(50) DEFAULT NULL COMMENT '情绪状态描述: 愉悦·充满活力 / 平静·内心安定 等',
    `emotion_composition` JSON DEFAULT NULL COMMENT '情绪成分分布 {"焦虑":0.4,"平静":0.3,...}',
    `composition_summary` TEXT DEFAULT NULL COMMENT '情绪成分总结文字',
    `trend_direction` VARCHAR(20) DEFAULT NULL COMMENT '趋势方向: 上升 / 下降 / 波动 / 平稳',
    `trend_description` TEXT DEFAULT NULL COMMENT '趋势分析文字',
    `ai_commentary` TEXT DEFAULT NULL COMMENT 'AI 专属治愈点评',
    `ai_insight` TEXT DEFAULT NULL COMMENT 'AI 深度洞察（模式识别）',
    `ai_suggestion` TEXT DEFAULT NULL COMMENT 'AI 针对性建议',
    `next_week_forecast` JSON DEFAULT NULL COMMENT '下周预测 {"riskLevel":"medium","riskDays":"周一","suggestion":"...","peakDay":"周三"}',
    `week_comparison` JSON DEFAULT NULL COMMENT '周环比数据 {"hasLastWeek":true,"avgScore":65,"mainTag":"平静","recordCount":5,"scoreDelta":3.5,"trendText":"..."}',
    `triggers` JSON DEFAULT NULL COMMENT '情绪触发点 [{"category":"工作","impact":"positive","count":3,"avgScore":75,"description":"..."}]',
    `high_point` JSON DEFAULT NULL COMMENT '本周高光时刻 {"date":"05-20","dayOfWeek":"周三","score":85,"emotion":"愉悦","inputText":"..."}',
    `low_point` JSON DEFAULT NULL COMMENT '本周低谷时刻 {"date":"05-17","dayOfWeek":"周一","score":38,"emotion":"焦虑","inputText":"..."}',
    `badges` JSON DEFAULT NULL COMMENT '成就徽章 ["全勤记录 🏆","低谷反弹 💪"]',
    `daily_scores` JSON DEFAULT NULL COMMENT '每日分数明细 [{"date":"05-17","dayOfWeek":"周一","score":42,"hasData":true,"emotion":"焦虑"}]',
    `turning_points` JSON DEFAULT NULL COMMENT '情绪拐点 [{"date":"05-19","dayOfWeek":"周三","score":72,"direction":1,"description":"..."}]',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_period` (`user_id`, `report_period`, `period_start`),
    UNIQUE KEY `uk_user_period` (`user_id`, `report_period`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI情绪报告表';
