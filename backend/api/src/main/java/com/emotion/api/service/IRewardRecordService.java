package com.emotion.api.service;

import com.emotion.api.repository.po.RewardRecord;

public interface IRewardRecordService {
    void saveRewardRecord(Long userId, String param,Integer count);
}