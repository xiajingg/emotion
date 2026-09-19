package com.emotion.api.dto;

import lombok.Data;

@Data
public class EmotionEventDTO {
    private Long interactionId;
    private String eventType;
    private String taskType;
    private String scenarioKey;
    private String replyStyle;
    private String extra;
}
