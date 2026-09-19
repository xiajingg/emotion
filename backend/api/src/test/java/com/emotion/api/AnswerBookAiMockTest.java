package com.emotion.api;

import com.emotion.api.service.AnswerBookService;
import com.emotion.api.service.OllamaChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 答案之书AI解读测试 - Mock Ollama服务
 */
@SpringBootTest
public class AnswerBookAiMockTest {

    @Autowired
    private AnswerBookService answerBookService;

    @MockBean
    private OllamaChatService ollamaChatService;

    /**
     * 测试：Mock Ollama返回正常结果
     */
    @Test
    public void testGenerateAiExplanationWithMockSuccess() {
        System.out.println("========== 测试：Mock Ollama返回正常结果 ==========");
        
        // Mock Ollama返回正常的AI解读
        String mockResponse = "这句话在告诉你，现在是做出改变的最佳时机。勇敢地迈出第一步，你会发现新的机遇正在等待着你。相信自己的直觉，它会指引你走向正确的方向。";
        when(ollamaChatService.chat(anyString())).thenReturn(mockResponse);
        
        String question = "我应该换工作吗？";
        String randomAnswer = "勇敢一点";
        Long recordId = 999L;  // 使用不存在的记录ID，避免更新数据库
        
        try {
            String aiExplanation = answerBookService.generateAiExplanationOnly(question, randomAnswer, recordId);
            
            System.out.println("问题: " + question);
            System.out.println("预设答案: " + randomAnswer);
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证返回结果
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "AI解读不应为空字符串");
            assertEquals(mockResponse, aiExplanation, "应该返回Mock的AI解读");
            
            // 验证Ollama被调用了一次
            verify(ollamaChatService, times(1)).chat(anyString());
            
            System.out.println("✅ 测试通过 - Mock Ollama正常工作");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("生成AI解读失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试：Mock Ollama返回空字符串
     */
    @Test
    public void testGenerateAiExplanationWithMockEmpty() {
        System.out.println("========== 测试：Mock Ollama返回空字符串 ==========");
        
        // Mock Ollama返回空字符串
        when(ollamaChatService.chat(anyString())).thenReturn("");
        
        String question = "我应该换工作吗？";
        String randomAnswer = "勇敢一点";
        Long recordId = 999L;
        
        try {
            String aiExplanation = answerBookService.generateAiExplanationOnly(question, randomAnswer, recordId);
            
            System.out.println("问题: " + question);
            System.out.println("预设答案: " + randomAnswer);
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证返回默认解读
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "即使Ollama返回空，也应该有默认解读");
            assertTrue(aiExplanation.contains("相信自己的内心"), "应该返回默认解读文案");
            
            // 验证Ollama被调用了一次
            verify(ollamaChatService, times(1)).chat(anyString());
            
            System.out.println("✅ 测试通过 - 空值降级处理正确");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("生成AI解读失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试：Mock Ollama返回null
     */
    @Test
    public void testGenerateAiExplanationWithMockNull() {
        System.out.println("========== 测试：Mock Ollama返回null ==========");
        
        // Mock Ollama返回null
        when(ollamaChatService.chat(anyString())).thenReturn(null);
        
        String question = "我应该换工作吗？";
        String randomAnswer = "勇敢一点";
        Long recordId = 999L;
        
        try {
            String aiExplanation = answerBookService.generateAiExplanationOnly(question, randomAnswer, recordId);
            
            System.out.println("问题: " + question);
            System.out.println("预设答案: " + randomAnswer);
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证返回默认解读
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "即使Ollama返回null，也应该有默认解读");
            assertTrue(aiExplanation.contains("相信自己的内心"), "应该返回默认解读文案");
            
            // 验证Ollama被调用了一次
            verify(ollamaChatService, times(1)).chat(anyString());
            
            System.out.println("✅ 测试通过 - null值降级处理正确");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("生成AI解读失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试：Mock Ollama抛出异常
     */
    @Test
    public void testGenerateAiExplanationWithMockException() {
        System.out.println("========== 测试：Mock Ollama抛出异常 ==========");
        
        // Mock Ollama抛出异常
        when(ollamaChatService.chat(anyString())).thenThrow(new RuntimeException("Ollama服务不可用"));
        
        String question = "我应该换工作吗？";
        String randomAnswer = "勇敢一点";
        Long recordId = 999L;
        
        try {
            String aiExplanation = answerBookService.generateAiExplanationOnly(question, randomAnswer, recordId);
            
            System.out.println("问题: " + question);
            System.out.println("预设答案: " + randomAnswer);
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证返回默认解读
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "即使Ollama异常，也应该有默认解读");
            assertTrue(aiExplanation.contains("相信自己的内心"), "应该返回默认解读文案");
            
            // 验证Ollama被调用了一次
            verify(ollamaChatService, times(1)).chat(anyString());
            
            System.out.println("✅ 测试通过 - 异常降级处理正确");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("生成AI解读失败: " + e.getMessage());
        }
        
        System.out.println();
    }
}
