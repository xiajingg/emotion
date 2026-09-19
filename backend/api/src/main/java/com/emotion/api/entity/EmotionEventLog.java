package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emotion_event_log")
public class EmotionEventLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long interactionId;
    private String eventType;
    private String taskType;
    private String scenarioKey;
    private String replyStyle;
    private String extra;
    private LocalDateTime createTime;
}
