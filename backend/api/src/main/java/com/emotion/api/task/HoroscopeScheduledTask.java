package com.emotion.api.task;

import com.emotion.api.service.HoroscopeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日星座运势定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoroscopeScheduledTask {

    private final HoroscopeGenerationService generationService;

    /**
     * 每天凌晨 0:30 执行，生成当天和明天的运势
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void generateDailyHoroscopes() {
        log.info("触发每日星座运势生成定时任务");
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        try {
            generationService.generateDailyHoroscopes(today);
            generationService.generateDailyHoroscopes(tomorrow);
        } catch (Exception e) {
            log.error("每日星座运势生成定时任务执行失败", e);
        }
    }
}
