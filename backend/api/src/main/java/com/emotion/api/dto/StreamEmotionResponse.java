package com.emotion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式情绪分析响应DTO
 * 用于SSE流式输出，逐步返回AI生成的内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEmotionResponse {
    
    /**
     * 事件类型：chunk(文本片段)、complete(完成)、error(错误)
     */
    private String type;
    
    /**
     * 数据内容
     * - type=chunk时：AI生成的文本片段
     * - type=complete时：完整的JSON字符串（包含score和suggestion）
     * - type=error时：错误信息
     */
    private String data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public static StreamEmotionResponse chunk(String content) {
        return new StreamEmotionResponse("chunk", content, System.currentTimeMillis());
    }
    
    public static StreamEmotionResponse complete(String fullJson) {
        return new StreamEmotionResponse("complete", fullJson, System.currentTimeMillis());
    }
    
    public static StreamEmotionResponse error(String message) {
        return new StreamEmotionResponse("error", message, System.currentTimeMillis());
    }
}
