package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.po.ActivityFreeUsage;
import com.emotion.api.repository.dao.rds.ActivityFreeUsageMapper;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.po.UserUsageLog;
import com.emotion.api.repository.mapper.UserUsageLogMapper;
import com.emotion.api.service.IActivityFreeUsageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.service.IUserFunctionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 记录每日免费抢使用次数成功的活动 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-12
 */
@Service
public class ActivityFreeUsageServiceImpl extends ServiceImpl<ActivityFreeUsageMapper, ActivityFreeUsage> implements IActivityFreeUsageService {


    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;
    
    @Autowired
    private UserUsageLogMapper userUsageLogMapper;
    /**
     * 保存活动免费抢次数
     * @param userId
     * @return
     */
//    @Async
    @Override
    public synchronized boolean saveActivityFreeUsage(Long userId) {
        ActivityFreeUsage activityFreeUsage = new ActivityFreeUsage();
        activityFreeUsage.setUserId(userId);
        activityFreeUsage.setActivityDate(LocalDate.now());
        activityFreeUsage.setRewardUsageCount(5);
        activityFreeUsage.setGrabTime(LocalDateTime.now());
        activityFreeUsage.setCreatedAt(LocalDateTime.now());
        save(activityFreeUsage);
        
        // 记录增加前的余额
        UserFunctionRecord userFunctionRecord = userFunctionRecordService.getUserFunctionRecord(userId);
        int balanceBefore = (int) (userFunctionRecord.getTotalUsageLimit() - userFunctionRecord.getUsedUsageCount());
        
        userFunctionRecord.setTotalUsageLimit(userFunctionRecord.getTotalUsageLimit() + 5);
        userFunctionRecordService.updateById(userFunctionRecord);
        
        // 记录增加后的余额
        int balanceAfter = (int) (userFunctionRecord.getTotalUsageLimit() - userFunctionRecord.getUsedUsageCount());
        
        // 记录次数变动日志
        UserUsageLog usageLog = new UserUsageLog();
        usageLog.setUserId(userId);
        usageLog.setOperationType("DAILY_BONUS");
        usageLog.setChangeAmount(5);
        usageLog.setBalanceBefore(balanceBefore);
        usageLog.setBalanceAfter(balanceAfter);
        usageLog.setRemark("每日名额领取");
        usageLog.setCreateTime(LocalDateTime.now());
        userUsageLogMapper.insert(usageLog);
        
        return true;
    }

    @Override
    public List<ActivityFreeUsage> getHistoryQuota() {
        List<ActivityFreeUsage> historyQuota = this.baseMapper.getHistoryQuota();
        return historyQuota == null ? Collections.emptyList() : historyQuota;
    }
}
