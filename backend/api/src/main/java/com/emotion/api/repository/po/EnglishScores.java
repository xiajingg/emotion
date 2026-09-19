package com.emotion.api.repository.po;

import java.math.BigDecimal;
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
 * 记录用户的三套题分数及加权总分
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_scores")
public class EnglishScores implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评分唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（关联user表）
     */
    private Integer userId;

    /**
     * 关联到english_main_text表的外键
     */
    private Integer mainTextId;

    /**
     * 单词问题得分
     */
    private BigDecimal wordScore;

    /**
     * 句子结构问题得分
     */
    private BigDecimal sentenceStructureScore;

    /**
     * 整段翻译得分
     */
    private BigDecimal fullTextTranslationScore;

    /**
     * 加权总分
     */
    private BigDecimal weightedTotalScore;

    /**
     * 评分时间
     */
    private LocalDateTime createdAt;


}
