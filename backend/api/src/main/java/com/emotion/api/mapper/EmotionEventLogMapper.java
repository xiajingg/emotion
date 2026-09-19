package com.emotion.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.entity.EmotionEventLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmotionEventLogMapper extends BaseMapper<EmotionEventLog> {
}
