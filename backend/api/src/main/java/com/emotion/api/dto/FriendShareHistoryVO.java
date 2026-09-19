package com.emotion.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class FriendShareHistoryVO {
    private Double myAverageScore;
    private Double friendAverageScore;
    private TrendData trendData;
    private List<TimelineItem> timeline;

    @Data
    public static class TrendData {
        private List<String> dates;
        private List<Integer> myScores;
        private List<Integer> friendScores;
    }

    @Data
    public static class TimelineItem {
        private String date;
        private List<EmotionRecord> records;
    }

    @Data
    public static class EmotionRecord {
        private String owner; // "me" or "friend"
        private String emotion;
        private Integer score;
        private String content;
        private String createTime;
    }
}
