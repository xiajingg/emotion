package com.emotion.api.service;

import com.emotion.api.entity.UserBehaviorLog;

/**
 * 用户行为分析Service
 */
public interface UserBehaviorLogService {
    
    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param behaviorType 行为类型: 1-查看每日补给, 2-查看星座运势, 3-点击探索菜单
     * @param notificationType 关联的提醒类型
     * @param hasUnread 当时是否有未读消息
     */
    void logBehavior(String userId, Integer behaviorType, Integer notificationType, boolean hasUnread);
}
