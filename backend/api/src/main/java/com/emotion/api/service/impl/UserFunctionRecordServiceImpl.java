package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.dao.rds.UserFunctionRecordMapper;
import com.emotion.api.service.IUserFunctionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 业务次数记录表 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-11-27
 */
@Service
public class UserFunctionRecordServiceImpl extends ServiceImpl<UserFunctionRecordMapper, UserFunctionRecord> implements IUserFunctionRecordService {

    @Override
    public UserFunctionRecord getUserFunctionRecord(Long userId) {
        LambdaQueryWrapper<UserFunctionRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFunctionRecord::getUserId, userId);
        UserFunctionRecord one = this.getOne(queryWrapper);
        if (one == null){
            one = new UserFunctionRecord();
            one.setUserId(userId);
            one.setDailyLimitTimes(3L);
            one.setUsedTimesToday(0L);
            one.setTotalUsageLimit(0L);
            one.setUsedUsageCount(0L);
            one.setTotalRemakeLimit(0);
            one.setUsedRemakeCount(0);
            one.setTxt2ImgDailyTotal(1L);
            one.setTxt2ImgDailyUsed(0L);
            one.setTxt2ImgTotal(0L);
            one.setTxt2ImgUsed(0L);
            one.setCreateTime(LocalDateTime.now());
            this.save(one);
        }
        return one;
    }
}
