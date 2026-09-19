package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 记录用户对单词问题的回答
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_user_word_answers")
public class EnglishUserWordAnswers implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户答案唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（关联user表）
     */
    private Integer userId;

    /**
     * 问题ID（关联english_word_questions表）
     */
    private Integer questionId;

    /**
     * 用户提供的答案
     */
    private String userAnswer;

    /**
     * 是否正确回答
     */
    private Boolean isCorrect;

    /**
     * 答案提交时间
     */
    private LocalDateTime createdAt;


}
