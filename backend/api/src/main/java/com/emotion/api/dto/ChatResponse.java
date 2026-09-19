package com.emotion.api.dto;

/**
 * 聊天响应DTO - 用于JSON Schema结构化输出
 */
public class ChatResponse {

    private String answer;
    private String category;
    private Double confidence;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String category, Double confidence) {
        this.answer = answer;
        this.category = category;
        this.confidence = confidence;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "answer='" + answer + '\'' +
                ", category='" + category + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
