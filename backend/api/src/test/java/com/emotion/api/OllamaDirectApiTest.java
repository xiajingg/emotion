package com.emotion.api;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 直接测试Ollama API（不通过ChatClient）
 */
@SpringBootTest
@ActiveProfiles("prod")  // 使用prod profile
public class OllamaDirectApiTest {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    /**
     * 测试直接使用OllamaChatModel
     */
    @Test
    public void testDirectOllamaChatModel() {
        System.out.println("========== 测试直接调用OllamaChatModel ==========");
        
        try {
            UserMessage userMessage = new UserMessage("你好，请回复一句话");
            
            System.out.println("发送请求到OllamaChatModel...");
            
            // 直接调用model
            ChatResponse response = ollamaChatModel.call(
                new org.springframework.ai.chat.prompt.Prompt(userMessage)
            );
            
            System.out.println("响应类型: " + response.getClass().getName());
            
            if (response != null && response.getResult() != null) {
                String content = response.getResult().getOutput().getText();
                System.out.println("AI回复: [" + content + "]");
                
                if (content == null || content.trim().isEmpty()) {
                    System.err.println("❌ AI返回空响应！");
                } else {
                    System.out.println("✅ AI正常响应！");
                }
            } else {
                System.err.println("❌ 响应或result为null！");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
