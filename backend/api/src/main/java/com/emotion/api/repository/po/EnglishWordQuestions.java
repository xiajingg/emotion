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
 * 存储从主文本中抽取的单词问题
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_word_questions")
public class EnglishWordQuestions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单词问题唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 单词
     */
    private String word;

    /**
     * 中文翻译
     */
    private String chineseTranslation;

    /**
     * 关联到english_main_text表的外键
     */
    private Integer mainTextId;

    /**
     * 问题创建时间
     */
    private LocalDateTime createdAt;


}
