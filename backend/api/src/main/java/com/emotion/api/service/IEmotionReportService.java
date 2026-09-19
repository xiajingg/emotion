package com.emotion.api.service;

import com.emotion.api.dto.WeeklyReportData;

/**
 * 情绪报告查询服务
 * 从 emotion_report 表读取 AI 预生成的报告，不复现计算
 */
public interface IEmotionReportService {

    /**
     * 获取指定周期的报告详情
     * 优先从 emotion_report 表读取；若不存在则实时调用 AI 生成
     */
    WeeklyReportData getReportDetail(Long userId, String startDate, String endDate);

    /**
     * 获取最新一期周报（用于首页入口等场景）
     */
    WeeklyReportData getLatestWeeklyReport(Long userId);
}
