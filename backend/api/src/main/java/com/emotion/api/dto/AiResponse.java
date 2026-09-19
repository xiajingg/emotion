package com.emotion.api.dto;

import lombok.Data;

@Data
public class AiResponse {
    private Data data;
    @lombok.Data
    public class Data {
        String emotion;
        String emotionRatio;
        String reminder;
    }
}
