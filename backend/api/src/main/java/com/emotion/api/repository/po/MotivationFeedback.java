package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("motivation_feedback")
public class MotivationFeedback {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键
    private Long motivationId; // 关联 daily_motivation 表的主键
    private Long userId; // 用户的唯一标识
    private Integer feedbackType; // 评价类型：1-LIKE 或 2-DISLIKE
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
}
