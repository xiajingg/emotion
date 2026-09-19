package com.emotion.api.dto;

import lombok.Data;

@Data
public class FriendLinkAnalysisVO {
    private String title;
    private String summary;
    private String suggestion;
    private String answerBook;
    private Integer meCount;
    private Integer friendCount;
    private Integer sameDayCount;
    private String updatedAt;
}
