package com.emotion.api.dto;

import lombok.Data;

@Data
public class UserTextInteractionsAnalysisData {
    private Integer totalCount;

    private Integer totalAvgScore;

    private Integer monthAvgScore;

    private Integer weekAvgScore;
}
