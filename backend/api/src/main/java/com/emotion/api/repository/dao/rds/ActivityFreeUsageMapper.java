package com.emotion.api.repository.dao.rds;

import com.emotion.api.repository.po.ActivityFreeUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 记录每日免费抢使用次数成功的活动 Mapper 接口
 * </p>
 *
 * @author xiajing
 * @since 2024-12-12
 */
public interface ActivityFreeUsageMapper extends BaseMapper<ActivityFreeUsage> {

    List<ActivityFreeUsage> getHistoryQuota();
}
