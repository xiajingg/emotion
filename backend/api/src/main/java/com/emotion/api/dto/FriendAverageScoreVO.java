package com.emotion.api.dto;

import lombok.Data;

/**
 * 好友平均分数响应
 */
@Data
public class FriendAverageScoreVO {
    private Double myAverageScore;
    private Double friendAverageScore;
}
