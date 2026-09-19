package com.emotion.api.task;

import com.emotion.api.service.EmotionReportGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 情绪周报生成定时任务
 *
 * 每周一凌晨 0:00 自动执行，为上周所有活跃用户生成 AI 情绪周报并存入 emotion_report 表。
 * 用户打开小程序时直接从表里读取，无需等待 AI 实时分析。
 */
@Slf4j
@Component
public class WeeklyReportGenerateTask {

    @Autowired
    private EmotionReportGenerateService generateService;

    /**
     * 每周一 00:00 生成上周周报
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void generateWeeklyReports() {
        log.info("===== 开始执行周报生成定时任务 =====");
        try {
            int count = generateService.batchGenerateWeeklyReports();
            log.info("===== 周报生成定时任务完成，本次生成 {} 份报告 =====", count);
        } catch (Exception e) {
            log.error("===== 周报生成定时任务异常 =====", e);
        }
    }
}
