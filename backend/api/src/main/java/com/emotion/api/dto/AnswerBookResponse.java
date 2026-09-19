package com.emotion.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答案之书响应DTO
 *
 * @author system
 * @since 2026-05-05
 */
@Data
public class AnswerBookResponse {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 用户提问内容
     */
    private String question;
    
    /**
     * 随机抽取的预设答案
     */
    private String randomAnswer;
    
    /**
     * AI生成的个性化解读
     */
    private String aiExplanation;
    
    /**
     * 消耗的使用次数
     */
    private Integer usageCount;
    
    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
