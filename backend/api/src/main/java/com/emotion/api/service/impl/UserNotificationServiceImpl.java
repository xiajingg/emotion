package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.entity.UserNotification;
import com.emotion.api.mapper.UserNotificationMapper;
import com.emotion.api.repository.dao.rds.WechatUserMapper;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.UserNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户消息提醒Service实现
 */
@Slf4j
@Service
public class UserNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification> 
        implements UserNotificationService {
    
    @Autowired
    private WechatUserMapper wechatUserMapper;
    
    @Override
    public Map<String, Integer> getUnreadCount(String userId) {
        // ✅ 新增：如果用户不存在，自动初始化
        ensureUserNotificationsExist(userId);
        
        Map<String, Integer> result = new HashMap<>();
        
        // 总未读数
        int totalCount = baseMapper.countUnreadByUserId(userId);
        
        // 每日补给未读数
        int dailyBonusUnread = baseMapper.countUnreadByType(userId, 1);
        
        // 星座运势未读数
        int horoscopeUnread = baseMapper.countUnreadByType(userId, 2);
        
        result.put("totalCount", totalCount);
        result.put("dailyBonusUnread", dailyBonusUnread);
        result.put("horoscopeUnread", horoscopeUnread);
        
        return result;
    }
    
    /**
     * 确保用户的通知记录存在（自动初始化）
     * 为新用户或首次查询的用户创建默认通知记录
     */
    private void ensureUserNotificationsExist(String userId) {
        // 检查用户是否已有通知记录。这里不能用未读数判断：
        // 老用户可能所有记录都已读，未读数为 0 但记录实际存在。
        int existingCount = baseMapper.countByUserId(userId);
        
        // 如果没有任何记录，说明是新用户，需要初始化
        if (existingCount == 0) {
            log.info("检测到新用户，自动初始化通知记录: userId={}", userId);
            
            List<UserNotification> notifications = new ArrayList<>();
            
            // 创建每日补给通知
            UserNotification dailyBonus = new UserNotification();
            dailyBonus.setUserId(userId);
            dailyBonus.setNotificationType(1);
            dailyBonus.setIsRead(1); // 默认为已读，避免新用户看到红点
            notifications.add(dailyBonus);
            
            // 创建星座运势通知
            UserNotification horoscope = new UserNotification();
            horoscope.setUserId(userId);
            horoscope.setNotificationType(2);
            horoscope.setIsRead(1); // 默认为已读
            notifications.add(horoscope);
            
            // 批量插入（使用 INSERT IGNORE 避免重复）
            baseMapper.batchInsertIgnore(notifications);
            log.info("用户通知记录初始化完成: userId={}", userId);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(String userId, Integer notificationType) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
               .eq(UserNotification::getNotificationType, notificationType)
               .eq(UserNotification::getIsRead, 0);
        
        UserNotification notification = new UserNotification();
        notification.setIsRead(1);
        notification.setUpdateTime(java.time.LocalDateTime.now()); // 手动设置更新时间
        
        int updated = this.getBaseMapper().update(notification, wrapper);
        return updated > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateNotifications(Integer notificationType, LocalDate triggerDate) {
        // 1. 获取目标用户列表
        List<String> userIds;
        if (notificationType == 1) {
            // 每日补给: 所有活跃用户(最近30天有登录的用户)
            userIds = getActiveUserIds();
        } else if (notificationType == 2) {
            // 星座运势: 已设置星座的用户
            userIds = getUserIdsWithConstellation();
        } else {
            throw new IllegalArgumentException("无效的提醒类型: " + notificationType);
        }
        
        if (userIds == null || userIds.isEmpty()) {
            log.warn("没有符合条件的用户, notificationType={}", notificationType);
            return 0;
        }
        
        // ✅ 优化：先尝试批量插入（新用户）
        List<UserNotification> notifications = userIds.stream()
                .map(userId -> {
                    UserNotification notification = new UserNotification();
                    notification.setUserId(userId);
                    notification.setNotificationType(notificationType);
                    notification.setIsRead(0);
                    return notification;
                })
                .collect(Collectors.toList());
        
        int insertedCount = baseMapper.batchInsertIgnore(notifications);
        
        // ✅ 优化：对于已有记录的用户，重置 is_read=0
        int updatedCount = 0;
        if (insertedCount < userIds.size()) {
            // 有部分用户已经存在记录，需要更新
            updatedCount = resetUserNotificationRead(notificationType, userIds);
        }
        
        log.info("批量创建/更新通知完成, type={}, 用户数={}, 插入数={}, 更新数={}", 
                notificationType, userIds.size(), insertedCount, updatedCount);
        
        return insertedCount + updatedCount;
    }
    
    /**
     * 重置已有用户的未读状态
     */
    private int resetUserNotificationRead(Integer notificationType, List<String> userIds) {
        // 分批更新，避免一次性更新太多数据
        int batchSize = 500;
        int totalUpdated = 0;
        
        for (int i = 0; i < userIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIds.size());
            List<String> batchUserIds = userIds.subList(i, end);
            
            // 更新这些用户的通知记录：重置为未读
            LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserNotification::getNotificationType, notificationType)
                   .in(UserNotification::getUserId, batchUserIds)
                   .eq(UserNotification::getIsRead, 1); // 只更新已读的记录
            
            UserNotification updateEntity = new UserNotification();
            updateEntity.setIsRead(0); // 重置为未读
            updateEntity.setUpdateTime(java.time.LocalDateTime.now());
            
            int updated = this.getBaseMapper().update(updateEntity, wrapper);
            totalUpdated += updated;
        }
        
        return totalUpdated;
    }
    
    /**
     * 获取活跃用户ID列表(最近30天有登录的用户)
     */
    private List<String> getActiveUserIds() {
        // 查询wechat_user表获取所有用户ID
        LambdaQueryWrapper<WechatUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(WechatUser::getId);
        // 可以添加条件: wrapper.ge(WechatUser::getLastLoginTime, LocalDate.now().minusDays(30));
        
        List<WechatUser> users = wechatUserMapper.selectList(wrapper);
        return users.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取已设置星座的用户ID列表
     */
    private List<String> getUserIdsWithConstellation() {
        LambdaQueryWrapper<WechatUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(WechatUser::getId)
               .isNotNull(WechatUser::getConstellation)
               .ne(WechatUser::getConstellation, "");
        
        List<WechatUser> users = wechatUserMapper.selectList(wrapper);
        return users.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.toList());
    }
}
