package com.emotion.api.util;

import cn.hutool.core.util.StrUtil;

public class QianfanAI {

    /**
     * AI对话
     * @param systemContent 不知道就填null
     * @param userContent
     * @return
     */
    public static String executeChat(String systemContent, String userContent) {
        return ZhipuAiUtil.chat(userContent, systemContent);
    }

    /**
     * 发送消息给聊天机器人
     * @param label 情绪标签
     * @param score 情绪分数
     * @param text 原话
     * @return 聊天机器人的回复内容
     */
    public static String emotionAnalysis(String label, String score, String text) {
        String systemContent = "你需要扮演一名好朋友的身份直接倾诉一段20字左右客观感觉. 你需要逐步分析分隔线以下内容, 下面内容的情绪分数范围定义的是1-100, 分数越低越消极, 分数越高越积极. 根据情绪,情绪分数和原话的内容, 如果情绪是消极的情绪就给与安慰, 如果情绪是积极的情绪就表达与你一起开心. 但是你不能说出你的身份";
        String userContent = "---------------------------------------\n" +
                             "情绪: " + label + ", \n" +
                             "情绪分数: " + score + ", \n" +
                             "原话: " + text + ". ";
        String result = executeChat(systemContent, userContent);
        return "{\"data\": {\"reminder\": \"" + result + "\"}}";
    }

    public static String executeChat(String systemContent,String assistant, String userContent) {
        String fullPrompt = systemContent + "\n" + assistant + "\n" + userContent;
        return ZhipuAiUtil.chat(fullPrompt);
    }

    public static void main(String[] args) {
        String s = QianfanAI.emotionAnalysis("消极", "50", "我好伤心");
        System.out.println(s);
    }
}
