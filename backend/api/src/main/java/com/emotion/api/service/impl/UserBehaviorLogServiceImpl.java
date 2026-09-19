package com.emotion.api.service.impl;

import com.emotion.api.entity.UserBehaviorLog;
import com.emotion.api.mapper.UserBehaviorLogMapper;
import com.emotion.api.service.UserBehaviorLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户行为分析Service实现
 */
@Slf4j
@Service
public class UserBehaviorLogServiceImpl implements UserBehaviorLogService {
    
    @Autowired
    private UserBehaviorLogMapper behaviorLogMapper;
    
    @Override
    public void logBehavior(String userId, Integer behaviorType, Integer notificationType, boolean hasUnread) {
        try {
            UserBehaviorLog log = new UserBehaviorLog();
            log.setUserId(userId);
            log.setBehaviorType(behaviorType);
            log.setNotificationType(notificationType);
            log.setHasUnread(hasUnread ? 1 : 0);
            log.setActionTime(LocalDateTime.now());
            
            behaviorLogMapper.insert(log);
        } catch (Exception e) {
            // 行为日志记录失败不影响主流程,只记录错误
            log.error("记录用户行为日志失败, userId={}, behaviorType={}", userId, behaviorType, e);
        }
    }
}
