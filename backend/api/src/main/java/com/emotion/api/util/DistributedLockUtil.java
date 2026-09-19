package com.emotion.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工具类
 */
@Slf4j
@Component
public class DistributedLockUtil {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final long LOCK_EXPIRE_TIME = 300; // 5分钟
    private static final long RETRY_INTERVAL = 100;   // 100ms
    private static final int MAX_RETRY_COUNT = 3;     // 最多重试3次
    
    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的key
     * @return 锁的唯一标识(null表示获取失败)
     */
    public String tryLock(String lockKey) {
        String lockValue = UUID.randomUUID().toString();
        
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(success)) {
                log.debug("成功获取分布式锁: {}", lockKey);
                return lockValue;
            }
            
            // 等待后重试
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取锁时被中断: {}", lockKey);
                return null;
            }
        }
        
        log.warn("获取分布式锁失败(已达最大重试次数): {}", lockKey);
        return null;
    }
    
    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     * @param lockValue 锁的唯一标识(防止误删其他实例的锁)
     */
    public void releaseLock(String lockKey, String lockValue) {
        if (lockValue == null) {
            return;
        }
        
        // 使用Lua脚本保证原子性
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        try {
            Object result = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Object>) connection -> 
                    connection.eval(
                        luaScript.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1,
                        lockKey.getBytes(),
                        lockValue.getBytes()
                    )
            );
            
            if (result != null && ((Number) result).longValue() > 0) {
                log.debug("成功释放分布式锁: {}", lockKey);
            } else {
                log.warn("释放分布式锁失败(可能已过期或被其他实例持有): {}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: {}", lockKey, e);
        }
    }
}
