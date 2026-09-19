package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.entity.UserNotification;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户消息提醒Service
 */
public interface UserNotificationService extends IService<UserNotification> {
    
    /**
     * 获取用户未读消息统计
     * @param userId 用户ID
     * @return Map包含: totalCount, dailyBonusUnread, horoscopeUnread
     */
    Map<String, Integer> getUnreadCount(String userId);
    
    /**
     * 标记消息为已读
     * @param userId 用户ID
     * @param notificationType 提醒类型: 1-每日补给, 2-星座运势
     * @return 是否成功
     */
    boolean markAsRead(String userId, Integer notificationType);
    
    /**
     * 批量创建通知记录(分布式任务调用)
     * @param notificationType 提醒类型
     * @param triggerDate 触发日期（保留参数以兼容定时任务，但实际不使用）
     * @return 插入的记录数
     */
    int batchCreateNotifications(Integer notificationType, LocalDate triggerDate);
}
