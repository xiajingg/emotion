package com.emotion.api;

import com.emotion.api.dto.EmotionAnalysisResponse;
import com.emotion.api.service.OllamaChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ollama情绪分析服务测试
 */
@SpringBootTest
public class OllamaChatServiceTest {

    @Autowired
    private OllamaChatService ollamaChatService;

    /**
     * 测试基本情绪分析功能
     */
    @Test
    public void testAnalyzeEmotion_Basic() {
        System.out.println("========== 测试基本情绪分析 ==========");
        
        String text = "今天工作很开心";
        EmotionAnalysisResponse response = ollamaChatService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        // 验证返回结果不为null
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        // 验证分数范围
        assertTrue(response.getScore() >= 1 && response.getScore() <= 100, 
                "分数应在1-100范围内，实际: " + response.getScore());
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试负面情绪分析
     */
    @Test
    public void testAnalyzeEmotion_Negative() {
        System.out.println("========== 测试负面情绪分析 ==========");
        
        String text = "今天心情很差，很沮丧";
        EmotionAnalysisResponse response = ollamaChatService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        // 负面情绪分数应该较低
        assertTrue(response.getScore() >= 1 && response.getScore() <= 100, 
                "分数应在1-100范围内，实际: " + response.getScore());
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试中性情绪分析
     */
    @Test
    public void testAnalyzeEmotion_Neutral() {
        System.out.println("========== 测试中性情绪分析 ==========");
        
        String text = "今天天气不错";
        EmotionAnalysisResponse response = ollamaChatService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试短文本
     */
    @Test
    public void testAnalyzeEmotion_ShortText() {
        System.out.println("========== 测试短文本 ==========");
        
        String text = "开心";
        EmotionAnalysisResponse response = ollamaChatService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试长文本
     */
    @Test
    public void testAnalyzeEmotion_LongText() {
        System.out.println("========== 测试长文本 ==========");
        
        String text = "今天完成了很多工作，虽然有点累，但是看到成果还是很开心的。" +
                "团队合作非常顺利，同事们都很支持我。晚上打算好好休息一下，" +
                "明天继续努力！";
        EmotionAnalysisResponse response = ollamaChatService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text.substring(0, Math.min(50, text.length())) + "...");
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }
}
