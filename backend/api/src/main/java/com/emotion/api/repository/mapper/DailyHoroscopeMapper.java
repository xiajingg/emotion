package com.emotion.api.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.DailyHoroscope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日星座运势Mapper接口
 *
 * @author system
 * @since 2026-05-09
 */
@Mapper
public interface DailyHoroscopeMapper extends BaseMapper<DailyHoroscope> {
}
