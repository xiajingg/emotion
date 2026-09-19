package com.emotion.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     * @param unit  时间单位
     */
    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    /**
     * 获取键对应的值
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 如果指定key的值是正数，则对其执行减一操作
     * 此方法用于在Redis中维护某个资源或计数器的正状态
     *
     * @param key Redis中的键名，用于标识要操作的数据
     * @return 如果操作后的值仍然是正数，则返回true；如果操作后的值小于等于0，则返回false
     */
    public boolean decrementIfPositive(String key) {
        // 对指定key的值执行减一操作
        Long result = redisTemplate.opsForValue().decrement(key);
        // 检查减一后的结果是否为null或小于0
        if (result == null || result < 0) {
            // 如果减到小于0，恢复原值并返回false
            redisTemplate.opsForValue().increment(key);
            return false;
        }
        // 如果结果为正数，返回true
        return true;
    }

    /**
     * 生产者方法：向Redis队列中添加消息
     *
     * 设计目的：
     * 1. 提供简单的消息发布机制
     * 2. 利用Redis List数据结构实现轻量级消息队列
     * 3. 支持分布式系统中的异步通信
     *
     * 使用场景：
     * - 任务分发：将待处理任务放入队列
     * - 解耦系统组件：生产者和消费者解耦
     * - 异步处理：快速响应，延迟处理
     *
     * @param queueName 队列名称，用于标识不同的消息通道
     * @param message 要发送的消息，可以是任意类型对象
     */
    public void sendMessage(String queueName, Object message) {
        redisTemplate.opsForList().leftPush(queueName, message);
    }

    /**
     * 消费者方法：从Redis队列中获取并移除消息
     * @param queueName 队列名称
     * @param timeout 等待超时时间，控制阻塞时间
     * @return 获取的消息，队列为空时返回null
     */
    public Object receiveMessage(String queueName, long timeout) {
        return redisTemplate.opsForList().rightPop(queueName, timeout, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将数据添加到Set集合中
     *
     * @param key 键
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean sadd(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().add(key, value));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断member元素是否是集合key的成员
     *
     * @param key 键
     * @param member 成员
     * @return true 存在 false不存在
     */
    public boolean sismember(String key, Object member) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, member));
    }

    /**
     * 将目标key的过期时间设置为与源key相同
     *
     * @param targetKey 目标key
     * @param sourceKey 源key
     */
    public void getExpire(String targetKey, String sourceKey) {
        Long expireTime = redisTemplate.getExpire(sourceKey);
        if (expireTime != null && expireTime > 0) {
            redisTemplate.expire(targetKey, expireTime, TimeUnit.SECONDS);
        }
    }

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey 锁的key
     * @param requestId 请求标识（用于确保只有持有锁的请求才能解锁）
     * @param expireTime 锁的过期时间（毫秒）
     * @return true表示获取锁成功，false表示获取锁失败
     */
    public boolean lock(String lockKey, String requestId, long expireTime) {
        try {
            Boolean success = redisTemplate.execute((RedisCallback<Boolean>) (connection) -> {
                // 尝试设置锁
                Boolean acquired = connection.setNX(lockKey.getBytes(), requestId.getBytes());
                if (Boolean.TRUE.equals(acquired)) {
                    // 设置成功，设置过期时间
                    connection.expire(lockKey.getBytes(), TimeUnit.MILLISECONDS.toSeconds(expireTime));
                }
                return acquired;
            });
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取分布式锁出错", e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * 使用Lua脚本确保原子性，只有持有锁的请求才能解锁
     *
     * @param lockKey 锁的key
     * @param requestId 请求标识
     * @return true表示释放锁成功，false表示释放锁失败
     */
    public boolean unlock(String lockKey, String requestId) {
        try {
            // Lua脚本，确保原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                          "return redis.call('del', KEYS[1]) " +
                          "else " +
                          "return 0 " +
                          "end";
            
            // 执行Lua脚本
            Long result = redisTemplate.execute((connection) -> {
                return connection.eval(
                    script.getBytes(),
                    ReturnType.INTEGER,
                    1,
                    lockKey.getBytes(),
                    requestId.getBytes()
                );
            }, true);
            
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("释放分布式锁出错", e);
            return false;
        }
    }

}
