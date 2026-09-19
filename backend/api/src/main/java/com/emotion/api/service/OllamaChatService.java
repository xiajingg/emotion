package com.emotion.api.service;

import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.ChatResponse;
import com.emotion.api.dto.EmotionAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Ollama聊天服务 - 兼容Spring AI 1.x
 */
@Slf4j
@Service
public class OllamaChatService {

    private final ChatClient chatClient;
    private final OllamaApi ollamaApi;

    public OllamaChatService(ChatClient chatClient, OllamaApi ollamaApi) {
        this.chatClient = chatClient;
        this.ollamaApi = ollamaApi;
    }

    /**
     * 简单对话
     *
     * @param message 用户消息
     * @return AI回复
     */
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 使用JSON Schema结构化输出的对话
     *
     * @param message 用户消息
     * @return 结构化的聊天响应
     */
    public ChatResponse chatWithJsonSchema(String message) {
        String prompt = message + "\n\n请以JSON格式回复，包含以下字段：answer(回答内容), category(问题分类: greeting/question/other), confidence(置信度0-1)";

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ChatResponse.class);
    }

    /**
     * 带系统提示的对话
     *
     * @param systemPrompt 系统提示
     * @param userMessage  用户消息
     * @return AI回复
     */
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 情绪分析 - 同步调用（关闭思考模式）
     *
     * @param text 用户输入的文本
     * @return 情绪分析结果（包含分数和建议）
     */
    public EmotionAnalysisResponse analyzeEmotion(String text) {
        log.info("开始同步情绪分析（关闭思考模式），文本长度: {}", text.length());
        long startTime = System.currentTimeMillis();
        
        // ✅ 简化Prompt + /no_think 指令（Spring AI 1.0.0-M6 临时方案）
        // ⚠️ 注意：M6版本仍不支持 additionalParameters 设置 think 参数，使用提示词指令作为替代
        String prompt = "请分析以下文本的情绪，返回JSON格式：{\"score\":整数,\"suggestion\":\"字符串\"}\n" +
                "评分：1-10绝望,11-20痛苦,21-30愤怒,31-40沮丧,41-50平静,51-60好奇,61-70满足,71-80开心,81-90兴奋,91-100狂喜\n" +
                "文本：" + text + "\n/no_think";

        try {
            String rawResponse = chatClient.prompt()
                    .user(prompt)
                    // 🔑 核心配置：强制关闭思考模式
                    // ⚠️ Spring AI 1.0.0-M6 不支持 additionalParameters 方法
                    // 💡 如需完全控制 think 参数，建议使用 OllamaDirectService
                    .options(OllamaOptions.builder().build())
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
            log.info("同步情绪分析完成，耗时: {}ms, 分数: {}, 建议: {}", 
                    elapsed, 
                    response != null ? response.getScore() : "null",
                    response != null ? response.getSuggestion() : "null");
            return response;
        } catch (Exception e) {
            log.error("同步情绪分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, e);
            throw new RuntimeException("情绪分析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从AI响应中提取JSON内容
     * 支持多种格式：纯JSON、Markdown代码块包裹的JSON
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        String trimmed = response.trim();
        
        // ✅ 方法1：提取Markdown代码块 ```json ... ```
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String jsonContent = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                // 移除可能存在的语言标识符（如 "json"）
                if (jsonContent.toLowerCase().startsWith("json")) {
                    jsonContent = jsonContent.substring(4).trim();
                }
                log.debug("从Markdown代码块中提取JSON");
                return jsonContent;
            }
        }
        
        // ✅ 方法2：直接查找JSON对象 { ... }
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            String jsonContent = trimmed.substring(braceStart, braceEnd + 1);
            log.debug("从文本中提取JSON对象");
            return jsonContent;
        }
        
        // ✅ 方法3：返回原始内容（假设已经是JSON）
        log.warn("未找到JSON格式，返回原始响应");
        return trimmed;
    }

    /**
     * 情绪分析 - 流式输出（关闭思考模式）
     * 逐步返回AI生成的内容，适合前端实时显示
     *
     * @param text 用户输入的文本
     * @return 流式的文本片段
     */
    public Flux<String> analyzeEmotionStream(String text) {
        log.info("开始流式情绪分析（关闭思考模式），文本长度: {}", text.length());
        long startTime = System.currentTimeMillis();
        
        String prompt = "请分析以下文本的情绪，并给出一个1-100的情绪分数和一句简短的建议或安慰语。\n" +
                "分数定义：1-10绝望，11-20痛苦，21-30愤怒，31-40沮丧，41-50平静，51-60好奇，61-70满足，71-80开心，81-90兴奋，91-100狂喜\n" +
                "文本内容：" + text + "\n\n" +
                "请以JSON格式返回，包含score(整数1-100)和suggestion(字符串)两个字段。\n/no_think";

        // 关键修复：使用 chatResponse() 而不是 content()
        // 然后从 ChatResponse 中提取内容
        return chatClient.prompt()
                .user(prompt)
                // 🔑 核心配置：强制关闭思考模式
                // ⚠️ Spring AI 1.0.0-M6 不支持 additionalParameters 方法
                // 💡 如需完全控制 think 参数，建议使用 OllamaDirectService
                .options(OllamaOptions.builder().build())
                .stream()
                .chatResponse()  // 返回 Flux<ChatResponse>
                .map(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    log.debug("收到流式数据块: {}", content);
                    return content;
                })
                .doOnError(error -> {
                    log.error("流式情绪分析失败，耗时: {}ms", System.currentTimeMillis() - startTime, error);
                })
                .doOnComplete(() -> {
                    log.info("流式情绪分析完成，总耗时: {}ms", System.currentTimeMillis() - startTime);
                });
    }
}
