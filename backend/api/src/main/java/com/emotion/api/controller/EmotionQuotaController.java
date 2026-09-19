package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.repository.po.ActivityFreeUsage;
import com.emotion.api.service.IActivityFreeUsageService;
import com.emotion.api.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.o;

/**
 * 情感分析名额控制器
 */
@Slf4j
@RestController
@RequestMapping("/emotion")
public class EmotionQuotaController {

    @Autowired
    private RedisUtil redisUtil;
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @Autowired
    private IActivityFreeUsageService activityFreeUsageService;

    @PostMapping("/quota/count")
    public BaseResult<Long> getQuota(@CurrentUser UserPrincipal userPrincipal) {
        String today = dateFormat.format(new Date());
        String redisKey = today + "emotion";
        Object o = redisUtil.get(redisKey);
        // 如果o为null,就返回0
        if (o == null) {
            return BaseResult.success(0L);
        }
        return BaseResult.success(Long.parseLong(o.toString()));
    }

    /**
     * 尝试获取名额
     * 如果当天还有剩余名额，将可用名额减1并返回成功
     * 如果没有剩余名额，返回失败
     */
    @PostMapping("/quota/acquire")
    public BaseResult<Boolean> acquireQuota(@CurrentUser UserPrincipal userPrincipal) {
        String today = dateFormat.format(new Date());
        String redisKey = today + "emotion";
        String acquiredUsersKey = today + "emotion:acquired_users";
        
        // 先检查是否有可用名额
        Object quotaObj = redisUtil.get(redisKey);
        if (quotaObj == null) {
            return BaseResult.error("400", "当日名额未开放");
        }
        int quota = Integer.parseInt(quotaObj.toString());
        if (quota <= 0) {
            return BaseResult.error("400", "当日名额已用完");
        }

        // 获取分布式锁，锁的key包含userId，超时时间设为10秒
        String lockKey = today + "emotion:lock:" + userPrincipal.getUserId();
        boolean lock = redisUtil.lock(lockKey, userPrincipal.getUserId().toString(), 10000);
        
        if (!lock) {
            log.warn("用户{}获取锁失败", userPrincipal.getUserId());
            return BaseResult.error("500", "操作太频繁，请稍后再试");
        }

        try {
            // 检查用户是否已经领取过名额
            if (redisUtil.sismember(acquiredUsersKey, userPrincipal.getUserId())) {
                log.info("用户{}今天已经领取过情感分析名额", userPrincipal.getUserId());
                return BaseResult.error("400", "您今天已经领取过情感分析名额");
            }

            // 再次检查并尝试获取名额（双重检查，确保名额充足）
            if (!redisUtil.hasKey(redisKey)) {
                return BaseResult.error("400", "当日名额已过期");
            }

            // 尝试获取名额（原子递减）
            boolean acquired = redisUtil.decrementIfPositive(redisKey);
            
            if (acquired) {
                Long userId = userPrincipal.getUserId();
                // 记录用户已领取
                redisUtil.sadd(acquiredUsersKey, userId);
                // 设置acquired_users的过期时间与名额key保持一致
                if (redisUtil.hasKey(redisKey)) {
                    redisUtil.getExpire(acquiredUsersKey, redisKey);
                }
                
                // 异步保存领取记录
                activityFreeUsageService.saveActivityFreeUsage(userId);
                
                log.info("用户{}成功获取情感分析名额", userId);
                return BaseResult.success(true);
            } else {
                log.info("用户{}获取情感分析名额失败，当日名额已用完", userPrincipal.getUserId());
                return BaseResult.error("400", "当日情感分析名额已用完");
            }
        } catch (Exception e) {
            log.error("用户{}获取情感分析名额时发生错误", userPrincipal.getUserId(), e);
            return BaseResult.error("500", "系统繁忙，请稍后再试");
        } finally {
            // 释放锁
            if (!redisUtil.unlock(lockKey, userPrincipal.getUserId().toString())) {
                log.warn("用户{}释放锁失败", userPrincipal.getUserId());
            }
        }
    }

    /**
     * 查询历史情感分析名额
     */
    @PostMapping("/quota/history")
    public BaseResult<List<ActivityFreeUsage>> getHistoryQuota(@CurrentUser UserPrincipal userPrincipal) {
        List<ActivityFreeUsage> historyList = activityFreeUsageService.getHistoryQuota();
        
        // ✅ 为所有用户处理显示名称（后端统一脱敏）
        String currentOpenId = userPrincipal.getUserOpenId();
        for (ActivityFreeUsage usage : historyList) {
            String displayName;
            
            // 优先使用昵称，如果昵称为空则脱敏openId
            if (usage.getNickname() != null && !usage.getNickname().isEmpty()) {
                displayName = usage.getNickname();
            } else {
                // ✅ 后端直接脱敏，确保原始 OpenID 不传输到前端
                displayName = maskOpenId(usage.getOpenId());
            }
            
            // 如果是当前用户，添加标记（前端用于高亮显示）
            if (currentOpenId != null && currentOpenId.equals(usage.getOpenId())) {
                displayName = displayName + "_IS_ME_";
            }
            
            usage.setNickname(displayName);
            // ✅ 清空原始 openId，防止泄露
            usage.setOpenId(null);
        }
        
        return BaseResult.success(historyList);
    }
    
    /**
     * ✅ 脱敏openId：保留前4位和后4位，中间用 *** 替换
     * 例如：oAbc1234Xyz5678 → oAbc***z5678
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.isEmpty()) {
            return "未知用户";
        }
        if (openId.length() <= 8) {
            return openId; // 太短无法脱敏，直接返回
        }
        return openId.substring(0, 4) + "***" + openId.substring(openId.length() - 4);
    }
} 