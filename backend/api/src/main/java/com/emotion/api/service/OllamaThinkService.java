package com.emotion.api.service;

import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.EmotionAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

/**
 * Ollama情绪分析服务 - 使用Spring AI官方API（关闭思考模式）
 */
@Slf4j
@Service
public class OllamaThinkService {

    private final ChatClient chatClient;

    public OllamaThinkService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 情绪分析 - 关闭思考模式
     * 
     * @param text 用户输入的文本
     * @return 情绪分析结果
     */
    public EmotionAnalysisResponse analyzeEmotion(String text) {
        log.info("开始情绪分析（关闭思考模式），文本长度: {}", text.length());
        long startTime = System.currentTimeMillis();

        // ✅ 构建Prompt
        String prompt = "请分析以下文本的情绪，返回JSON格式：{\"score\":整数,\"suggestion\":\"字符串\"}\n" +
                "评分：1-10绝望,11-20痛苦,21-30愤怒,31-40沮丧,41-50平静,51-60好奇,61-70满足,71-80开心,81-90兴奋,91-100狂喜\n" +
                "文本：" + text;

        try {
            // ✅ 关键：使用 OllamaOptions 关闭思考模式
            // ⚠️ 注意：Spring AI 1.0.0-M5 不支持 .with() 方法
            // 需要使用底层 API 或等待更高版本
            String rawResponse = chatClient.prompt()
                    .user(prompt)
                    // ❌ M5版本不支持此方法，暂时注释
                    // .options(OllamaOptions.create().with("think", false))
                    .call()
                    .content();

            log.info("Ollama原始响应: [{}]", rawResponse);

            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                log.error("Ollama返回空响应");
                throw new RuntimeException("AI服务返回空响应");
            }

            // ✅ 提取并清理JSON
            String jsonStr = extractJsonFromResponse(rawResponse);
            log.info("提取后的JSON: {}", jsonStr);

            // ✅ 手动解析为DTO对象
            EmotionAnalysisResponse response = JSONUtil.toBean(jsonStr, EmotionAnalysisResponse.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("情绪分析完成（关闭思考），耗时: {}ms, 分数: {}, 建议: {}", 
                    elapsed, 
                    response != null ? response.getScore() : "null",
                    response != null ? response.getSuggestion() : "null");
            return response;
        } catch (Exception e) {
            log.error("情绪分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("情绪分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String trimmed = response.trim();

        // 方法1：提取Markdown代码块 ```json ... ```
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String jsonContent = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                if (jsonContent.toLowerCase().startsWith("json")) {
                    jsonContent = jsonContent.substring(4).trim();
                }
                return jsonContent;
            }
        }

        // 方法2：直接查找JSON对象 { ... }
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1);
        }

        // 方法3：返回原始内容
        return trimmed;
    }
}
