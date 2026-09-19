package com.emotion.api.dto;

import lombok.Data;

@Data
public class EmotionFavoriteDTO {
    private Long interactionId;
    private String scenarioKey;
    private String replyStyle;
    private String replyText;
}
