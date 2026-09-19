package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI情绪报告表
 * 由定时任务每周一 0 点自动调用 AI 生成，前端直接读取
 * report_period 支持 WEEK / MONTH
 */
@Data
@TableName("emotion_report")
public class EmotionReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 报告周期: WEEK / MONTH */
    private String reportPeriod;

    /** 周期开始日期 */
    private LocalDate periodStart;

    /** 周期结束日期 */
    private LocalDate periodEnd;

    /** 日期范围展示: 05-17至05-23 */
    private String dateRange;

    /** 记录次数 */
    private Integer recordCount;

    /** 打卡天数 */
    private Integer checkInDays;

    /** 主打情绪标签 */
    private String mainEmotion;

    /** 情绪状态描述 */
    private String emotionState;

    /** 情绪成分分布 JSON */
    private String emotionComposition;

    /** 情绪成分总结文字 */
    private String compositionSummary;

    /** 趋势方向 */
    private String trendDirection;

    /** 趋势分析文字 */
    private String trendDescription;

    /** AI 专属治愈点评 */
    private String aiCommentary;

    /** AI 深度洞察 */
    private String aiInsight;

    /** AI 针对性建议 */
    private String aiSuggestion;

    /** 下周预测 JSON */
    private String nextWeekForecast;

    /** 周环比数据 JSON */
    private String weekComparison;

    /** 情绪触发点 JSON */
    private String triggers;

    /** 高光时刻 JSON */
    private String highPoint;

    /** 低谷时刻 JSON */
    private String lowPoint;

    /** 成就徽章 JSON */
    private String badges;

    /** 每日分数 JSON */
    private String dailyScores;

    /** 情绪拐点 JSON */
    private String turningPoints;

    /** 状态: 1-正常 0-已删除 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
