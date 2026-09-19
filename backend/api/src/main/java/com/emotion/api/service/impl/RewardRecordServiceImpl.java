package com.emotion.api.service.impl;

import com.emotion.api.repository.dao.rds.RewardRecordMapper;
import com.emotion.api.repository.po.RewardRecord;
import com.emotion.api.service.IRewardRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RewardRecordServiceImpl implements IRewardRecordService {

    @Autowired
    private RewardRecordMapper rewardRecordMapper;

    @Override
    public void saveRewardRecord(Long userId, String param, Integer count) {
        RewardRecord record = new RewardRecord();
        record.setUserId(userId);
        record.setParam(param);
        record.setCreateTime(new Date());
        record.setCount(count); // 初始化次数为1
        rewardRecordMapper.insert(record);
    }
}