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
 * 星座运势提醒定时任务
 * 每天00:00执行
 */
@Slf4j
@Component
public class HoroscopeNotificationTask {
    
    @Autowired
    private UserNotificationService notificationService;
    
    @Autowired
    private DistributedLockUtil distributedLockUtil;
    
    @Autowired
    private ScheduledTaskLogMapper taskLogMapper;
    
    private static final String TASK_KEY = "horoscope_notification";
    private static final String TASK_NAME = "星座运势提醒任务";
    
    /**
     * 每天00:00执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
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
            int createdCount = notificationService.batchCreateNotifications(2, today);
            
            long duration = System.currentTimeMillis() - startTime;
            
            taskLog.setStatus(1); // 成功
            taskLog.setDurationMs(duration);
            
            log.info("[{}] 执行成功, 创建通知数={}, 耗时={}ms", TASK_NAME, createdCount, duration);
            
        } catch (Exception e) {
            log.error("[{}] 执行失败", TASK_NAME, e);
            
            taskLog.setStatus(2); // 失败
            taskLog.setErrorMsg(e.getMessage());
            
        } finally {
            taskLogMapper.insert(taskLog);
            
            if (lockValue != null) {
                distributedLockUtil.releaseLock(lockKey, lockValue);
            }
        }
    }
}
