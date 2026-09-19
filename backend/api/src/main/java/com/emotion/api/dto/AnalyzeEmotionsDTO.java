package com.emotion.api.dto;

import lombok.Data;

@Data
public class AnalyzeEmotionsDTO {
    /**
     * 分数
     */
    private Integer score;
    /**
     * 建议
     */
    private String suggestion;
}
