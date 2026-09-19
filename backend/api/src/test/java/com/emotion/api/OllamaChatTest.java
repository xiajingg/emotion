package com.emotion.api;

import com.emotion.api.dto.ChatResponse;
import com.emotion.api.service.OllamaChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ollama聊天测试
 */
@SpringBootTest
public class OllamaChatTest {

    @Autowired
    private OllamaChatService ollamaChatService;

    /**
     * 测试简单对话
     */
    @Test
    public void testSimpleChat() {
        System.out.println("========== 测试简单对话 ==========");
        String response = ollamaChatService.chat("你好，请介绍一下你自己");
        System.out.println("AI回复: " + response);
        System.out.println();
    }

    /**
     * 测试JSON Schema结构化输出
     */
    @Test
    public void testChatWithJsonSchema() {
        System.out.println("========== 测试JSON Schema结构化输出 ==========");
        ChatResponse response = ollamaChatService.chatWithJsonSchema("什么是人工智能？");
        System.out.println("结构化响应:");
        System.out.println("  回答: " + response.getAnswer());
        System.out.println("  分类: " + response.getCategory());
        System.out.println("  置信度: " + response.getConfidence());
        System.out.println();
    }

    /**
     * 测试带系统提示的对话
     */
    @Test
    public void testChatWithSystemPrompt() {
        System.out.println("========== 测试带系统提示的对话 ==========");
        String systemPrompt = "你是一个专业的技术顾问，擅长用简洁清晰的语言解释技术概念。";
        String userMessage = "请解释一下什么是Spring AI";
        String response = ollamaChatService.chatWithSystemPrompt(systemPrompt, userMessage);
        System.out.println("系统提示: " + systemPrompt);
        System.out.println("用户问题: " + userMessage);
        System.out.println("AI回复: " + response);
        System.out.println();
    }

    /**
     * 测试多轮对话场景
     */
    @Test
    public void testMultipleQuestions() {
        System.out.println("========== 测试多个问题 ==========");

        String[] questions = {
            "Java和Python有什么区别？",
            "什么是微服务架构？",
            "如何优化数据库查询性能？"
        };

        for (int i = 0; i < questions.length; i++) {
            System.out.println("问题 " + (i + 1) + ": " + questions[i]);
            String response = ollamaChatService.chat(questions[i]);
            System.out.println("AI回复: " + response);
            System.out.println("---");
        }
    }

    /**
     * 测试JSON Schema在业务场景中的应用
     */
    @Test
    public void testBusinessScenario() {
        System.out.println("========== 测试业务场景（房产咨询） ==========");

        String question = "我想在北京朝阳区租一套两居室，预算8000元左右，有什么建议？";
        ChatResponse response = ollamaChatService.chatWithJsonSchema(question);

        System.out.println("用户问题: " + question);
        System.out.println("结构化响应:");
        System.out.println("  回答: " + response.getAnswer());
        System.out.println("  分类: " + response.getCategory());
        System.out.println("  置信度: " + response.getConfidence());
        System.out.println();
    }
}
