package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.service.UserNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试Controller - 用于手动触发定时任务
 */
@Slf4j
@RestController
@RequestMapping("/test/api/v1/notification")
public class NotificationTestController {
    
    @Autowired
    private UserNotificationService notificationService;
    
    /**
     * 手动触发每日补给提醒
     */
    @PostMapping("/trigger-daily-bonus")
    public BaseResult<Map<String, Object>> triggerDailyBonus() {
        try {
            LocalDate today = LocalDate.now();
            int count = notificationService.batchCreateNotifications(1, today);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "每日补给提醒创建成功");
            result.put("count", count);
            result.put("date", today.toString());
            
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("触发每日补给提醒失败", e);
            return BaseResult.error("500", "触发失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发星座运势提醒
     */
    @PostMapping("/trigger-horoscope")
    public BaseResult<Map<String, Object>> triggerHoroscope() {
        try {
            LocalDate today = LocalDate.now();
            int count = notificationService.batchCreateNotifications(2, today);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "星座运势提醒创建成功");
            result.put("count", count);
            result.put("date", today.toString());
            
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("触发星座运势提醒失败", e);
            return BaseResult.error("500", "触发失败: " + e.getMessage());
        }
    }
    
    /**
     * 同时触发两个提醒
     */
    @PostMapping("/trigger-all")
    public BaseResult<Map<String, Object>> triggerAll() {
        try {
            LocalDate today = LocalDate.now();
            
            int bonusCount = notificationService.batchCreateNotifications(1, today);
            int horoscopeCount = notificationService.batchCreateNotifications(2, today);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "所有提醒创建成功");
            result.put("dailyBonusCount", bonusCount);
            result.put("horoscopeCount", horoscopeCount);
            result.put("totalCount", bonusCount + horoscopeCount);
            result.put("date", today.toString());
            
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("触发所有提醒失败", e);
            return BaseResult.error("500", "触发失败: " + e.getMessage());
        }
    }
}
