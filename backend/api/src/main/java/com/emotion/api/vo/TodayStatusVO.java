package com.emotion.api.vo;

import lombok.Data;

/**
 * 今日打卡状态VO
 */
@Data
public class TodayStatusVO {
    /**
     * 今日是否已打卡
     */
    private Boolean hasCheckedIn;

    /**
     * 连续打卡天数
     */
    private Integer consecutiveDays;

    /**
     * 剩余补签次数
     */
    private Integer remainingRemakeCount;
}
