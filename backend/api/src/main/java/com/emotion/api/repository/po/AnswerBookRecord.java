package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答案之书记录实体类
 *
 * @author system
 * @since 2026-05-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("answer_book_records")
public class AnswerBookRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户提问内容
     */
    @TableField("question")
    private String question;

    /**
     * 随机抽取的预设答案
     */
    @TableField("random_answer")
    private String randomAnswer;

    /**
     * AI生成的个性化解读
     */
    @TableField("ai_explanation")
    private String aiExplanation;

    /**
     * 消耗的使用次数
     */
    @TableField("usage_count")
    private Integer usageCount;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;
}
