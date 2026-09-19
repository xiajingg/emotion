package com.emotion.api.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 情绪周报 - 深度体检报告
 */
@Data
public class WeeklyReportData {

    // ===== 基础信息 =====
    private String dateRange;        // 日期范围：2026-05-16至2026-05-22
    private String startDate;        // 开始日期
    private String endDate;          // 结束日期
    private int recordCount;         // 记录次数
    private int checkInDays;         // 打卡天数（有记录的天数）

    // ===== 核心评分 =====
    private Double averageScore;     // 本周平均分
    private String emotionState;     // 情绪状态：平静·舒缓
    private String mainTag;          // 主打情绪标签
    private int scoreRank;           // 本周排名（与自己历史比）：70 = 优于70%的周

    // ===== 情绪成分 =====
    /** 情绪标签分布，key=标签名, value=占比(小数，如0.4表示40%) */
    private Map<String, Double> emotionComposition;
    /** 情绪成分描述文本 */
    private String compositionSummary;

    // ===== 趋势分析 =====
    private List<Map<String, Object>> dailyScores; // 每日分数 [{date, dayOfWeek, score, hasData, emotion, text}]
    private String trendDirection;   // 趋势方向：上升/下降/波动/平稳
    private String trendDescription; // 趋势分析文本
    /** 情绪拐点 [{date, dayOfWeek, description, direction: 1/-1}] */
    private List<Map<String, Object>> turningPoints;

    // ===== 高光与低谷 =====
    /** {date, dayOfWeek, score, emotion, inputText} */
    private Map<String, Object> highPoint;
    /** {date, dayOfWeek, score, emotion, inputText} */
    private Map<String, Object> lowPoint;

    // ===== 周环比 =====
    /** {hasLastWeek, avgScore, mainTag, recordCount, scoreDelta, trendText} */
    private Map<String, Object> weekComparison;

    // ===== 情绪触发点分析 =====
    /** [{category: "工作", impact: "positive", count: 3, avgScore: 75, description: "..."}] */
    private List<Map<String, Object>> triggers;

    // ===== AI 洞察 =====
    private String aiCommentary;     // AI 专属治愈点评
    private String aiInsight;        // AI 深度洞察：模式识别
    private String aiSuggestion;     // AI 针对性建议

    // ===== 下周预测 =====
    /** {riskLevel, riskDays, suggestion, peakDay} */
    private Map<String, Object> nextWeekForecast;

    // ===== 成就与徽章 =====
    /** 本周获得的徽章 ["连续记录7天", "情绪过山车", "最低谷反弹"] */
    private List<String> badges;

    // ===== 历史数据（给前端画图用） =====
    private List<HistoryEmotion> events;

    // ===== 兼容旧字段（前端过渡期） =====
    private String mainEvents;
}
