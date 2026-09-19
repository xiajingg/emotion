package com.emotion.api.dto;

/**
 * 情绪分析响应DTO - 用于Ollama JSON Schema结构化输出
 */
public class EmotionAnalysisResponse {

    /**
     * 情绪分数 (1-100)
     */
    private Integer score;

    /**
     * 情绪标签（如：情绪透支、轻松期待等）
     */
    private String emotion;

    /**
     * 建议/安慰语
     */
    private String suggestion;

    public EmotionAnalysisResponse() {
    }

    public EmotionAnalysisResponse(Integer score, String emotion, String suggestion) {
        this.score = score;
        this.emotion = emotion;
        this.suggestion = suggestion;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "EmotionAnalysisResponse{" +
                "score=" + score +
                ", emotion='" + emotion + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}
