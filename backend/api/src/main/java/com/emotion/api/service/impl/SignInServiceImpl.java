package com.emotion.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.dto.QuickCheckInDTO;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.po.UserTextInteraction;
import com.emotion.api.repository.po.UserUsageLog;
import com.emotion.api.repository.mapper.UserUsageLogMapper;
import com.emotion.api.service.IRewardRecordService;
import com.emotion.api.service.ISignInService;
import com.emotion.api.service.IUserFunctionRecordService;
import com.emotion.api.util.RedisUtil;
import com.emotion.api.vo.QuickCheckInVO;
import com.emotion.api.vo.TodayStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 签到服务实现类
 */
@Slf4j
@Service
public class SignInServiceImpl implements ISignInService {

    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;

    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;
    
    @Autowired
    private UserUsageLogMapper userUsageLogMapper;

    @Autowired
    private IRewardRecordService rewardRecordService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 情绪类型到分数的映射
     */
    private Double mapMoodToScore(Integer moodType) {
        switch (moodType) {
            case 1: return 75.0;  // 开心
            case 2: return 45.0;  // 平静
            case 3: return 35.0;  // 悲伤
            case 4: return 25.0;  // 愤怒
            case 5: return 15.0;  // 疲惫
            default: return 50.0; // 默认中性
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuickCheckInVO quickCheckIn(Long userId, QuickCheckInDTO dto) {
        log.info("用户 {} 快速打卡，表情: {}, 类型: {}", userId, dto.getMoodEmoji(), dto.getMoodType());

        // 1. 使用Redis分布式锁防止重复打卡
        String lockKey = "checkin:" + userId + ":" + LocalDate.now();
        boolean locked = redisUtil.lock(lockKey, userId.toString(), 86400);
        if (!locked) {
            throw new RuntimeException("今日已打卡");
        }

        try {
            // 2. 再次检查今日是否已打卡（双重检查）
            if (hasCheckedInToday(userId)) {
                throw new RuntimeException("今日已打卡");
            }

            // 3. 保存打卡记录到 user_text_interactions 表
            UserTextInteraction record = new UserTextInteraction();
            record.setUserId(userId);
            record.setType(1);  // 签到类型
            record.setInputText(dto.getMoodEmoji());  // 存储表情符号
            record.setScore(mapMoodToScore(dto.getMoodType()));  // 映射情绪分数
            record.setSignInTime(LocalDateTime.now());
            record.setSupplementarySignIn(0);  // 非补签
            record.setCreateTime(LocalDateTime.now());
            if (dto.getLongitude() != null) {
                record.setLongitude(dto.getLongitude());
            }
            if (dto.getLatitude() != null) {
                record.setLatitude(dto.getLatitude());
            }
            userTextInteractionMapper.insert(record);

            // 4. 赠送1次分析次数
            UserFunctionRecord functionRecord = userFunctionRecordService.getUserFunctionRecord(userId);
            
            // 记录增加前的余额
            int balanceBefore = (int) (functionRecord.getTotalUsageLimit() - functionRecord.getUsedUsageCount());
            
            functionRecord.setTotalUsageLimit(functionRecord.getTotalUsageLimit() + 1);
            userFunctionRecordService.updateById(functionRecord);
            
            // 记录增加后的余额
            int balanceAfter = (int) (functionRecord.getTotalUsageLimit() - functionRecord.getUsedUsageCount());
            
            // 记录次数变动日志
            UserUsageLog usageLog = new UserUsageLog();
            usageLog.setUserId(userId);
            usageLog.setOperationType("SIGN_IN");
            usageLog.setChangeAmount(1);
            usageLog.setBalanceBefore(balanceBefore);
            usageLog.setBalanceAfter(balanceAfter);
            usageLog.setRemark("每日签到奖励");
            usageLog.setCreateTime(LocalDateTime.now());
            userUsageLogMapper.insert(usageLog);

            // 5. 记录奖励
            rewardRecordService.saveRewardRecord(userId, "daily_checkin", 1);

            // 6. 计算连续打卡天数
            int consecutiveDays = calculateConsecutiveDays(userId);

            // 7. 返回结果
            QuickCheckInVO vo = new QuickCheckInVO();
            vo.setCheckInSuccess(true);
            vo.setRewardCount(1);
            vo.setConsecutiveDays(consecutiveDays);
            vo.setIsFirstToday(true);

            log.info("用户 {} 打卡成功，连续天数: {}", userId, consecutiveDays);
            return vo;

        } catch (Exception e) {
            // 发生异常时释放锁
            redisUtil.unlock(lockKey, userId.toString());
            log.error("用户 {} 打卡失败: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public TodayStatusVO getTodayStatus(Long userId) {
        TodayStatusVO vo = new TodayStatusVO();
        vo.setHasCheckedIn(hasCheckedInToday(userId));
        vo.setConsecutiveDays(calculateConsecutiveDays(userId));

        // 获取剩余补签次数
        UserFunctionRecord record = userFunctionRecordService.getUserFunctionRecord(userId);
        int remainingRemake = record.getTotalRemakeLimit() - record.getUsedRemakeCount();
        vo.setRemainingRemakeCount(Math.max(0, remainingRemake));

        return vo;
    }

    @Override
    public boolean hasCheckedInToday(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTextInteraction::getUserId, userId)
                .eq(UserTextInteraction::getType, 1)  // 签到类型
                .ge(UserTextInteraction::getSignInTime, today.atStartOfDay())
                .lt(UserTextInteraction::getSignInTime, today.plusDays(1).atStartOfDay());

        List<UserTextInteraction> records = userTextInteractionMapper.selectList(queryWrapper);
        return CollUtil.isNotEmpty(records);
    }

    @Override
    public int calculateConsecutiveDays(Long userId) {
        // 查询该用户所有签到记录，按时间倒序
        LambdaQueryWrapper<UserTextInteraction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTextInteraction::getUserId, userId)
                .eq(UserTextInteraction::getType, 1)  // 签到类型
                .orderByDesc(UserTextInteraction::getSignInTime);

        List<UserTextInteraction> records = userTextInteractionMapper.selectList(queryWrapper);

        if (CollUtil.isEmpty(records)) {
            return 0;
        }

        // 计算连续天数
        int consecutiveDays = 1;
        LocalDate lastDate = records.get(0).getSignInTime().toLocalDate();
        LocalDate today = LocalDate.now();

        // 如果最后一次打卡不是今天或昨天，则连续天数为0
        if (ChronoUnit.DAYS.between(lastDate, today) > 1) {
            return 0;
        }

        // 从第二次打卡开始遍历，计算连续天数
        for (int i = 1; i < records.size(); i++) {
            LocalDate currentDate = records.get(i).getSignInTime().toLocalDate();
            long daysBetween = ChronoUnit.DAYS.between(currentDate, lastDate);

            if (daysBetween == 1) {
                // 连续的一天
                consecutiveDays++;
                lastDate = currentDate;
            } else if (daysBetween == 0) {
                // 同一天多次打卡，跳过
                continue;
            } else {
                // 不连续，中断
                break;
            }
        }

        return consecutiveDays;
    }
}
