package com.emotion.api.service;

import com.emotion.api.repository.po.ActivityFreeUsage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 记录每日免费抢使用次数成功的活动 服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-12
 */
public interface IActivityFreeUsageService extends IService<ActivityFreeUsage> {

    /**
     * 保存活动免费抢次数
     */
    boolean saveActivityFreeUsage(Long userId);

    List<ActivityFreeUsage> getHistoryQuota();
}
