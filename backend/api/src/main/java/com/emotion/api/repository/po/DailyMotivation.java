package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_motivation")
public class DailyMotivation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键
    private LocalDate date; // 对应的日期
    private String content; // "毒鸡汤"句子内容
    private Integer totalLikes; // 总赞次数
    private Integer totalDislikes; // 总踩次数
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
}
