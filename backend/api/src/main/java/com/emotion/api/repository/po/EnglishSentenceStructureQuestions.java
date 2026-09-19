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
 * 存储用于句子结构判断的选择题
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_sentence_structure_questions")
public class EnglishSentenceStructureQuestions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 从文本中抽取的一句话或几句话作为题目
     */
    private String sentence;

    /**
     * 句子结构类型（如 SVO 表示主谓宾）
     */
    private String structureType;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 关联到english_main_text表的外键
     */
    private Integer mainTextId;

    /**
     * 题目创建时间
     */
    private LocalDateTime createdAt;


}
