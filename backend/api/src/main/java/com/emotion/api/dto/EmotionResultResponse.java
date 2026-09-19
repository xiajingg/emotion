package com.emotion.api.dto;

import java.util.List;

/**
 * 情绪解压卡响应DTO
 * 单次输入生成情绪识别、解压动作和回复建议。
 */
public class EmotionResultResponse {

    /**
     * 情绪标签（4-8字，有网感的精准概括）
     */
    private String emotion_tag;

    /**
     * AI 锐评/共情（30-50字，第一人称闺蜜视角）
     */
    private String ai_comment;

    /**
     * 即时解压动作（固定返回3条）
     */
    private List<String> relief_actions;

    /**
     * 多种风格回复建议
     */
    private Replies replies;

    /**
     * 答案之书（4-8字，玄学指引）
     */
    private String answer_book;

    /**
     * 任务类型：REPLY_RESCUE/SLEEP_RUMINATION/PRESSURE_RESCUE/GENERAL
     */
    private String task_type;

    /**
     * 用户真正被卡住或被刺痛的点
     */
    private String pain_point;

    /**
     * 60 秒内可执行的稳定动作
     */
    private String stabilize_action;

    /**
     * 当前不建议说出口的话，避免扩大冲突
     */
    private String dont_say;

    /**
     * 下次遇到同类场景时的提醒
     */
    private String next_time_tip;

    public EmotionResultResponse() {
    }

    public static class Replies {
        private String high_eq;
        private String crazy;
        private String gentle;
        private String sarcastic;
        private String safe;
        private String firm;
        private String short_reply;

        public String getHigh_eq() { return high_eq; }
        public void setHigh_eq(String high_eq) { this.high_eq = high_eq; }
        public String getCrazy() { return crazy; }
        public void setCrazy(String crazy) { this.crazy = crazy; }
        public String getGentle() { return gentle; }
        public void setGentle(String gentle) { this.gentle = gentle; }
        public String getSarcastic() { return sarcastic; }
        public void setSarcastic(String sarcastic) { this.sarcastic = sarcastic; }
        public String getSafe() { return safe; }
        public void setSafe(String safe) { this.safe = safe; }
        public String getFirm() { return firm; }
        public void setFirm(String firm) { this.firm = firm; }
        public String getShort_reply() { return short_reply; }
        public void setShort_reply(String short_reply) { this.short_reply = short_reply; }

        @Override
        public String toString() {
            return "Replies{high_eq='" + high_eq + "', crazy='" + crazy + "', gentle='" + gentle + "', sarcastic='" + sarcastic + "', safe='" + safe + "', firm='" + firm + "', short_reply='" + short_reply + "'}";
        }
    }

    public String getEmotion_tag() { return emotion_tag; }
    public void setEmotion_tag(String emotion_tag) { this.emotion_tag = emotion_tag; }
    public String getAi_comment() { return ai_comment; }
    public void setAi_comment(String ai_comment) { this.ai_comment = ai_comment; }
    public List<String> getRelief_actions() { return relief_actions; }
    public void setRelief_actions(List<String> relief_actions) { this.relief_actions = relief_actions; }
    public Replies getReplies() { return replies; }
    public void setReplies(Replies replies) { this.replies = replies; }
    public String getAnswer_book() { return answer_book; }
    public void setAnswer_book(String answer_book) { this.answer_book = answer_book; }
    public String getTask_type() { return task_type; }
    public void setTask_type(String task_type) { this.task_type = task_type; }
    public String getPain_point() { return pain_point; }
    public void setPain_point(String pain_point) { this.pain_point = pain_point; }
    public String getStabilize_action() { return stabilize_action; }
    public void setStabilize_action(String stabilize_action) { this.stabilize_action = stabilize_action; }
    public String getDont_say() { return dont_say; }
    public void setDont_say(String dont_say) { this.dont_say = dont_say; }
    public String getNext_time_tip() { return next_time_tip; }
    public void setNext_time_tip(String next_time_tip) { this.next_time_tip = next_time_tip; }

    @Override
    public String toString() {
        return "EmotionResultResponse{" +
                "emotion_tag='" + emotion_tag + '\'' +
                ", ai_comment='" + ai_comment + '\'' +
                ", relief_actions=" + relief_actions +
                ", replies=" + replies +
                ", answer_book='" + answer_book + '\'' +
                ", task_type='" + task_type + '\'' +
                ", pain_point='" + pain_point + '\'' +
                ", stabilize_action='" + stabilize_action + '\'' +
                ", dont_say='" + dont_say + '\'' +
                ", next_time_tip='" + next_time_tip + '\'' +
                '}';
    }
}
