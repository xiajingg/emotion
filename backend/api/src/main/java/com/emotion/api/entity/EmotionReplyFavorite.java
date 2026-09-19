package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emotion_reply_favorite")
public class EmotionReplyFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long interactionId;
    private String scenarioKey;
    private String replyStyle;
    private String replyText;
    private LocalDateTime createTime;
}
