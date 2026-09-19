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
 * <p>
 *
 * </p>
 *
 * @author dyz
 * @since 2024-08-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_text_interactions")
public class UserTextInteraction implements Serializable {
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
    private String longitude;

    @TableField("latitude")
    private String latitude;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    // 类型：0-正常，1-补签
    @TableField("type")
    private int type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("sign_in_time")
    private LocalDateTime signInTime;

    // 补签到字段，0-未补签，1-已补签
    @TableField("sup_sign_in")
    private int supplementarySignIn;

    // 分析AI情绪的分本
    @TableField("ai_text")
    private String aiText;
}
