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
 * 记录用户对整段英文的翻译尝试
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_user_full_text_translations")
public class EnglishUserFullTextTranslations implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户翻译尝试唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（关联user表）
     */
    private Integer userId;

    /**
     * 文本材料ID（关联english_main_text表）
     */
    private Integer mainTextId;

    /**
     * 用户提供的翻译文本
     */
    private String translatedText;

    /**
     * 翻译提交时间
     */
    private LocalDateTime createdAt;


}
