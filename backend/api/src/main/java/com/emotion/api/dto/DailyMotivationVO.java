package com.emotion.api.dto;

import lombok.Data;

@Data
public class DailyMotivationVO {
    private Boolean result;

    private Integer totalLikes;

    private Integer totalDislikes;

    private String motivationContent;
}
