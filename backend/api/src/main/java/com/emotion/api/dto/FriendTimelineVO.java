package com.emotion.api.dto;

import lombok.Data;
import java.util.List;

/**
 * 好友时间线响应
 */
@Data
public class FriendTimelineVO {
    private List<TimelineItem> timeline;

    @Data
    public static class TimelineItem {
        private String date;
        private List<EmotionRecord> records;
    }

    @Data
    public static class EmotionRecord {
        private Long id;
        private String owner; // "me" or "friend"
        private String emotion;
        private Integer score;
        private String content;
        private Boolean doodle;
        private java.util.List<Long> imgId;
        private java.util.List<String> imageUrls;
        private String aiResponse; // ✅ AI 回复内容
        private String createTime;
    }
}
