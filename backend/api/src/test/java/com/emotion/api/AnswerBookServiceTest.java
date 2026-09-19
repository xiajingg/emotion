package com.emotion.api;

import com.emotion.api.dto.AnswerBookRequest;
import com.emotion.api.dto.AnswerBookResponse;
import com.emotion.api.service.AnswerBookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 答案之书服务测试
 */
@SpringBootTest
public class AnswerBookServiceTest {

    @Autowired
    private AnswerBookService answerBookService;

    /**
     * 测试第一步：获取预设答案
     */
    @Test
    public void testGetRandomAnswerOnly() {
        System.out.println("========== 测试第一步：获取预设答案 ==========");
        
        AnswerBookRequest request = new AnswerBookRequest();
        request.setQuestion("我应该换工作吗？");
        
        // 使用测试用户ID（需要确保数据库中存在该用户的功能记录）
        Long testUserId = 1L;
        
        try {
            AnswerBookResponse response = answerBookService.getRandomAnswerOnly(testUserId, request);
            
            System.out.println("问题: " + response.getQuestion());
            System.out.println("预设答案: " + response.getRandomAnswer());
            System.out.println("AI解读: " + response.getAiExplanation());
            System.out.println("记录ID: " + response.getId());
            
            // 验证返回结果
            assertNotNull(response, "响应不应为null");
            assertNotNull(response.getQuestion(), "问题不应为null");
            assertNotNull(response.getRandomAnswer(), "预设答案不应为null");
            assertNotNull(response.getId(), "记录ID不应为null");
            
            // 第一步不应该有AI解读
            assertEquals("", response.getAiExplanation(), "第一步AI解读应为空字符串");
            
            System.out.println("✅ 测试通过");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("获取预设答案失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试第二步：生成AI解读
     */
    @Test
    public void testGenerateAiExplanationOnly() {
        System.out.println("========== 测试第二步：生成AI解读 ==========");
        
        String question = "我应该换工作吗？";
        String randomAnswer = "勇敢一点";
        Long recordId = 1L;  // 使用一个已存在的记录ID
        
        try {
            String aiExplanation = answerBookService.generateAiExplanationOnly(question, randomAnswer, recordId);
            
            System.out.println("问题: " + question);
            System.out.println("预设答案: " + randomAnswer);
            System.out.println("记录ID: " + recordId);
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证返回结果
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "AI解读不应为空字符串");
            assertTrue(aiExplanation.length() > 10, "AI解读长度应大于10个字符，实际: " + aiExplanation.length());
            
            System.out.println("✅ 测试通过");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("生成AI解读失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试完整的两步流程
     */
    @Test
    public void testTwoStepProcess() {
        System.out.println("========== 测试完整两步流程 ==========");
        
        String question = "我最近感到很迷茫";
        Long testUserId = 1L;
        
        try {
            // 第一步：获取预设答案
            System.out.println("--- 第一步：获取预设答案 ---");
            AnswerBookRequest request1 = new AnswerBookRequest();
            request1.setQuestion(question);
            
            AnswerBookResponse step1Response = answerBookService.getRandomAnswerOnly(testUserId, request1);
            
            System.out.println("预设答案: " + step1Response.getRandomAnswer());
            System.out.println("记录ID: " + step1Response.getId());
            
            assertNotNull(step1Response.getId(), "记录ID不应为null");
            assertNotNull(step1Response.getRandomAnswer(), "预设答案不应为null");
            
            // 第二步：生成AI解读
            System.out.println("\n--- 第二步：生成AI解读 ---");
            String aiExplanation = answerBookService.generateAiExplanationOnly(
                question, 
                step1Response.getRandomAnswer(), 
                step1Response.getId()
            );
            
            System.out.println("AI解读长度: " + (aiExplanation != null ? aiExplanation.length() : 0));
            System.out.println("AI解读内容: " + aiExplanation);
            
            // 验证最终结果
            assertNotNull(aiExplanation, "AI解读不应为null");
            assertFalse(aiExplanation.trim().isEmpty(), "AI解读不应为空字符串");
            
            System.out.println("\n✅ 完整流程测试通过");
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("完整流程测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试异常情况：空问题
     */
    @Test
    public void testEmptyQuestion() {
        System.out.println("========== 测试异常情况：空问题 ==========");
        
        AnswerBookRequest request = new AnswerBookRequest();
        request.setQuestion("");
        
        Long testUserId = 1L;
        
        try {
            answerBookService.getRandomAnswerOnly(testUserId, request);
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            System.out.println("✅ 正确捕获异常: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * 测试异常情况：问题过短
     */
    @Test
    public void testShortQuestion() {
        System.out.println("========== 测试异常情况：问题过短 ==========");
        
        AnswerBookRequest request = new AnswerBookRequest();
        request.setQuestion("你好");  // 少于5个字
        
        Long testUserId = 1L;
        
        try {
            answerBookService.getRandomAnswerOnly(testUserId, request);
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            System.out.println("✅ 正确捕获异常: " + e.getMessage());
        }
        
        System.out.println();
    }
}
