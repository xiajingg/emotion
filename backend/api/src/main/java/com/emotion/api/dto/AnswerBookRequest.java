package com.emotion.api.dto;

import lombok.Data;

/**
 * 答案之书请求DTO
 *
 * @author system
 * @since 2026-05-05
 */
@Data
public class AnswerBookRequest {
    
    /**
     * 记录ID（用于第二步更新）
     */
    private Long id;
    
    /**
     * 用户提问内容
     */
    private String question;
    
    /**
     * 预设答案（用于第二步AI分析）
     */
    private String randomAnswer;
}
