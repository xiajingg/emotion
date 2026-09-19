package com.emotion.api.vo;

import lombok.Data;

/**
 * 快速打卡响应VO
 */
@Data
public class QuickCheckInVO {
    /**
     * 打卡是否成功
     */
    private Boolean checkInSuccess;

    /**
     * 奖励次数
     */
    private Integer rewardCount;

    /**
     * 连续打卡天数
     */
    private Integer consecutiveDays;

    /**
     * 是否今日首次打卡
     */
    private Boolean isFirstToday;
}
