package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日星座运势实体类
 *
 * @author system
 * @since 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("daily_horoscope")
public class DailyHoroscope implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 星座名称 (如: Aries, Taurus)
     */
    @TableField("zodiac_sign")
    private String zodiacSign;

    /**
     * 综合运势分数 (1-100)
     */
    @TableField("score")
    private Integer score;

    /**
     * AI生成的运势内容
     */
    @TableField("content")
    private String content;

    /**
     * 天体专业分析
     */
    @TableField("astro_analysis")
    private String astroAnalysis;

    /**
     * 情感运势分数 (1-100)
     */
    @TableField("love_fortune")
    private Integer loveFortune;

    /**
     * 财富运势分数 (1-100)
     */
    @TableField("wealth_fortune")
    private Integer wealthFortune;

    /**
     * 事业运势分数 (1-100)
     */
    @TableField("career_fortune")
    private Integer careerFortune;

    /**
     * 宜忌事项
     */
    @TableField("dos_and_donts")
    private String dosAndDonts;

    /**
     * AI 综合建议
     */
    @TableField("ai_advice")
    private String aiAdvice;

    /**
     * 使用的AI模型
     */
    @TableField("ai_model")
    private String aiModel;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;
}
