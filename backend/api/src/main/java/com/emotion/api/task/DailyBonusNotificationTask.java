package com.emotion.api.task;

import com.emotion.api.entity.ScheduledTaskLog;
import com.emotion.api.mapper.ScheduledTaskLogMapper;
import com.emotion.api.service.UserNotificationService;
import com.emotion.api.util.DistributedLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日补给提醒定时任务
 * 每天12:00执行
 */
@Slf4j
@Component
public class DailyBonusNotificationTask {
    
    @Autowired
    private UserNotificationService notificationService;
    
    @Autowired
    private DistributedLockUtil distributedLockUtil;
    
    @Autowired
    private ScheduledTaskLogMapper taskLogMapper;
    
    private static final String TASK_KEY = "daily_bonus_notification";
    private static final String TASK_NAME = "每日补给提醒任务";
    
    /**
     * 每天12:00执行
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void execute() {
        LocalDate today = LocalDate.now();
        String lockKey = "lock:scheduled:" + TASK_KEY + ":" + today;
        String lockValue = null;
        
        ScheduledTaskLog taskLog = new ScheduledTaskLog();
        taskLog.setTaskName(TASK_NAME);
        taskLog.setTaskKey(TASK_KEY);
        taskLog.setExecuteDate(today);
        taskLog.setExecuteTime(LocalDateTime.now());
        taskLog.setStatus(0); // 执行中
        
        try {
            // 获取本机实例ID
            String instanceId = InetAddress.getLocalHost().getHostAddress();
            taskLog.setInstanceId(instanceId);
            
            long startTime = System.currentTimeMillis();
            
            // 尝试获取分布式锁
            lockValue = distributedLockUtil.tryLock(lockKey);
            if (lockValue == null) {
                log.warn("[{}] 其他实例正在执行,跳过本次任务", TASK_NAME);
                taskLog.setStatus(2);
                taskLog.setErrorMsg("获取分布式锁失败");
                return;
            }
            
            log.info("[{}] 开始执行, instanceId={}", TASK_NAME, instanceId);
            
            // 执行业务逻辑
            int createdCount = notificationService.batchCreateNotifications(1, today);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录成功日志
            taskLog.setStatus(1); // 成功
            taskLog.setDurationMs(duration);
            
            log.info("[{}] 执行成功, 创建通知数={}, 耗时={}ms", TASK_NAME, createdCount, duration);
            
        } catch (Exception e) {
            log.error("[{}] 执行失败", TASK_NAME, e);
            
            // 记录失败日志
            taskLog.setStatus(2); // 失败
            taskLog.setErrorMsg(e.getMessage());
            
        } finally {
            // 保存任务日志
            taskLogMapper.insert(taskLog);
            
            // 释放分布式锁
            if (lockValue != null) {
                distributedLockUtil.releaseLock(lockKey, lockValue);
            }
        }
    }
}
