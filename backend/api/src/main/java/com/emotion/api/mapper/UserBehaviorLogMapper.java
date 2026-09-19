package com.emotion.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为分析Mapper
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {
}
