package com.emotion.api.service;

import com.emotion.api.dto.WeeklyReportData;
import com.emotion.api.entity.UserWeeklyReportSubscription;

public interface IWeeklyReportService {

    /**
     * 记录/刷新订阅授权（每次用户弹窗授权后调用）
     * 不再有"取消订阅"概念，授权是一次性的
     */
    void saveSubscription(Long userId, String openId, String templateId);

    /**
     * 检查本周是否已有有效授权（推送后即失效）
     */
    boolean checkSubscription(Long userId, String templateId);

    /**
     * 检查用户是否曾经订阅过（查行为日志表，用于区分首次/再次订阅）
     */
    boolean hasEverSubscribed(Long userId);

    /**
     * 生成上周周报数据（给定时任务推送用）
     */
    WeeklyReportData generateWeeklyReport(Long userId);

    /**
     * 推送一条订阅消息给指定用户
     *
     * @return true-推送成功，false-推送失败
     */
    boolean sendWeeklyReport(UserWeeklyReportSubscription subscription);

    /**
     * 批量推送周报（定时任务调用）
     */
    void batchSendWeeklyReports();

    /**
     * 生成指定日期范围的周报详情（给详情页使用）
     */
    WeeklyReportData generateWeeklyReportDetail(Long userId, String startDate, String endDate);
}
