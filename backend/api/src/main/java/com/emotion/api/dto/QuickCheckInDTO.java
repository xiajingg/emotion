package com.emotion.api.dto;

import lombok.Data;

/**
 * 快速打卡请求DTO
 */
@Data
public class QuickCheckInDTO {
    /**
     * 表情符号：😊😐😢😡😴
     */
    private String moodEmoji;

    /**
     * 情绪类型：1-开心, 2-平静, 3-悲伤, 4-愤怒, 5-疲惫
     */
    private Integer moodType;

    /**
     * 经度（可选）
     */
    private String longitude;

    /**
     * 纬度（可选）
     */
    private String latitude;
}
