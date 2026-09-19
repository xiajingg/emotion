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
 * 存储英文学习材料的主表
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("english_main_text")
public class EnglishMainText implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主文本唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 英文段落内容
     */
    private String textContent;

    /**
     * 创建该段落的用户ID（关联user表）
     */
    private Integer createdBy;

    /**
     * 文本材料创建时间
     */
    private LocalDateTime createdAt;


}
