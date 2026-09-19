package com.emotion.api.service;

import com.emotion.api.EmotionApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

/**
 * 通知功能测试 - 手动触发提醒生成
 */
@Slf4j
@SpringBootTest(classes = EmotionApplication.class)
public class NotificationTriggerTest {
    
    @Autowired
    private UserNotificationService notificationService;
    
    /**
     * 触发每日补给提醒(模拟12点定时任务)
     */
    @Test
    public void testTriggerDailyBonus() {
        log.info("========== 开始触发每日补给提醒 ==========");
        
        LocalDate today = LocalDate.now();
        int count = notificationService.batchCreateNotifications(1, today);
        
        log.info("✅ 每日补给提醒创建完成,共为 {} 个用户创建了今日提醒", count);
        log.info("日期: {}", today);
        log.info("========================================");
    }
    
    /**
     * 触发星座运势提醒(模拟0点定时任务)
     */
    @Test
    public void testTriggerHoroscope() {
        log.info("========== 开始触发星座运势提醒 ==========");
        
        LocalDate today = LocalDate.now();
        int count = notificationService.batchCreateNotifications(2, today);
        
        log.info("✅ 星座运势提醒创建完成,共为 {} 个用户创建了今日提醒", count);
        log.info("日期: {}", today);
        log.info("========================================");
    }
    
    /**
     * 同时触发两个提醒
     */
    @Test
    public void testTriggerAll() {
        log.info("========== 开始触发所有提醒 ==========");
        
        LocalDate today = LocalDate.now();
        
        // 触发每日补给
        int dailyBonusCount = notificationService.batchCreateNotifications(1, today);
        log.info("✅ 每日补给提醒: {} 条", dailyBonusCount);
        
        // 触发星座运势
        int horoscopeCount = notificationService.batchCreateNotifications(2, today);
        log.info("✅ 星座运势提醒: {} 条", horoscopeCount);
        
        log.info("📊 总计创建提醒: {} 条", dailyBonusCount + horoscopeCount);
        log.info("=======================================");
    }
}
