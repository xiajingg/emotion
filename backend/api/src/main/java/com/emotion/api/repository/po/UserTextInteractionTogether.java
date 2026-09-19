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
import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author dyz
 * @since 2024-08-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_text_interactions_together")
public class UserTextInteractionTogether implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("input_text")
    private String inputText;

    @TableField("ai_response")
    private String aiResponse;

    @TableField("score")
    private Double score;

    @TableField("longitude")
    private Double longitude;

    @TableField("latitude")
    private Double latitude;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    @TableField("unique_Code")
    private String uniqueCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("sign_in_time")
    private LocalDateTime signInTime;

    @TableField("sup_sign_in")
    private int supplementarySignIn;
}
