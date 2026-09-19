package com.emotion.api.controller;

import com.emotion.api.dto.ChatResponse;
import com.emotion.api.service.OllamaChatService;
import org.springframework.web.bind.annotation.*;

/**
 * Ollama聊天控制器
 */
@RestController
@RequestMapping("/api/ollama")
public class OllamaChatController {

    private final OllamaChatService ollamaChatService;

    public OllamaChatController(OllamaChatService ollamaChatService) {
        this.ollamaChatService = ollamaChatService;
    }

    /**
     * 简单对话接口
     *
     * @param message 用户消息
     * @return AI回复
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return ollamaChatService.chat(message);
    }

    /**
     * POST方式简单对话
     *
     * @param request 请求体，包含message字段
     * @return AI回复
     */
    @PostMapping("/chat")
    public String chatPost(@RequestBody ChatRequest request) {
        return ollamaChatService.chat(request.getMessage());
    }

    /**
     * 使用JSON Schema结构化输出的对话
     *
     * @param message 用户消息
     * @return 结构化的聊天响应
     */
    @GetMapping("/chat/structured")
    public ChatResponse chatWithJsonSchema(@RequestParam String message) {
        return ollamaChatService.chatWithJsonSchema(message);
    }

    /**
     * POST方式使用JSON Schema结构化输出
     *
     * @param request 请求体，包含message字段
     * @return 结构化的聊天响应
     */
    @PostMapping("/chat/structured")
    public ChatResponse chatWithJsonSchemaPost(@RequestBody ChatRequest request) {
        return ollamaChatService.chatWithJsonSchema(request.getMessage());
    }

    /**
     * 带系统提示的对话
     *
     * @param request 请求体，包含systemPrompt和userMessage字段
     * @return AI回复
     */
    @PostMapping("/chat/system")
    public String chatWithSystemPrompt(@RequestBody SystemChatRequest request) {
        return ollamaChatService.chatWithSystemPrompt(
                request.getSystemPrompt(),
                request.getUserMessage()
        );
    }

    /**
     * 内部类 - 聊天请求
     */
    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 内部类 - 系统聊天请求
     */
    public static class SystemChatRequest {
        private String systemPrompt;
        private String userMessage;

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public void setUserMessage(String userMessage) {
            this.userMessage = userMessage;
        }
    }
}
