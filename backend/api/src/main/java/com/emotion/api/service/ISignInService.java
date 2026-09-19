package com.emotion.api.service;

import com.emotion.api.dto.QuickCheckInDTO;
import com.emotion.api.vo.QuickCheckInVO;
import com.emotion.api.vo.TodayStatusVO;

/**
 * 签到服务接口
 */
public interface ISignInService {

    /**
     * 快速打卡
     * @param userId 用户ID
     * @param dto 打卡请求参数
     * @return 打卡结果
     */
    QuickCheckInVO quickCheckIn(Long userId, QuickCheckInDTO dto);

    /**
     * 查询今日打卡状态
     * @param userId 用户ID
     * @return 今日状态
     */
    TodayStatusVO getTodayStatus(Long userId);

    /**
     * 检查用户今日是否已打卡
     * @param userId 用户ID
     * @return true-已打卡，false-未打卡
     */
    boolean hasCheckedInToday(Long userId);

    /**
     * 计算连续打卡天数
     * @param userId 用户ID
     * @return 连续天数
     */
    int calculateConsecutiveDays(Long userId);
}
