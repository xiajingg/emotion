package com.emotion.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.entity.ScheduledTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务执行记录Mapper
 */
@Mapper
public interface ScheduledTaskLogMapper extends BaseMapper<ScheduledTaskLog> {
}
