package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.service.UserBehaviorLogService;
import com.emotion.api.service.UserNotificationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户消息提醒Controller
 */
@Slf4j
@RestController
@RequestMapping("/user/api/v1/notification")
public class UserNotificationController {
    
    @Autowired
    private UserNotificationService notificationService;
    
    @Autowired
    private UserBehaviorLogService behaviorLogService;
    
    /**
     * 获取未读消息统计
     */
    @GetMapping("/unread-count")
    public BaseResult<Map<String, Integer>> getUnreadCount(@CurrentUser UserPrincipal userPrincipal) {
        String userId = String.valueOf(userPrincipal.getUserId());
        
        Map<String, Integer> unreadCount = notificationService.getUnreadCount(userId);
        
        return BaseResult.success(unreadCount);
    }
    
    /**
     * 标记消息为已读
     */
    @PostMapping("/mark-read")
    public BaseResult<Boolean> markAsRead(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody MarkReadRequest request) {
        
        String userId = String.valueOf(userPrincipal.getUserId());
        Integer notificationType = request.getNotificationType();
        
        // 先查询是否有未读
        Map<String, Integer> unreadCount = notificationService.getUnreadCount(userId);
        boolean hasUnread = false;
        if (notificationType == 1) {
            hasUnread = unreadCount.getOrDefault("dailyBonusUnread", 0) > 0;
        } else if (notificationType == 2) {
            hasUnread = unreadCount.getOrDefault("horoscopeUnread", 0) > 0;
        }
        
        boolean success = notificationService.markAsRead(userId, notificationType);
        
        if (success) {
            // 记录用户行为(用于后续分析)
            behaviorLogService.logBehavior(
                userId, 
                notificationType == 1 ? 1 : 2, // 行为类型: 1-查看每日补给, 2-查看星座运势
                notificationType,              // 提醒类型
                hasUnread                      // 当时有未读
            );
        }
        
        return BaseResult.success(success);
    }
    
    /**
     * 请求对象
     */
    @Data
    public static class MarkReadRequest {
        private Integer notificationType;
    }
}
