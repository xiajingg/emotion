package com.emotion.api.task;

import com.emotion.api.service.IWeeklyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeeklyReportTask {
    
    @Autowired
    private IWeeklyReportService weeklyReportService;
    
    /**
     * 每周一上午9:00发送周报
     * Cron表达式：秒 分 时 日 月 周
     * 0 0 9 ? * MON 表示每周一上午9点
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    public void sendWeeklyReports() {
        log.info("===== 开始执行周报定时任务 =====");
        
        try {
            weeklyReportService.batchSendWeeklyReports();
            log.info("===== 周报定时任务执行完成 =====");
        } catch (Exception e) {
            log.error("===== 周报定时任务执行失败 =====", e);
        }
    }
}
