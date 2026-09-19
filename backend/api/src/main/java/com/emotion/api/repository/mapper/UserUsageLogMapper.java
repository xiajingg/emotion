package com.emotion.api.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.UserUsageLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserUsageLogMapper extends BaseMapper<UserUsageLog> {
}
