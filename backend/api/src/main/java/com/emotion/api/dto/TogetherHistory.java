package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class TogetherHistory {
    private String time;

    private List<HistoryEmotion> historyEmotions;
}
