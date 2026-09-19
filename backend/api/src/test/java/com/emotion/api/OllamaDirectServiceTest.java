package com.emotion.api;

import com.emotion.api.dto.EmotionAnalysisResponse;
import com.emotion.api.service.OllamaDirectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试Ollama直接API调用（不使用Spring AI）
 */
@SpringBootTest
@ActiveProfiles("prod")
public class OllamaDirectServiceTest {

    @Autowired
    private OllamaDirectService ollamaDirectService;

    /**
     * 测试基本情绪分析功能
     */
    @Test
    public void testAnalyzeEmotion_Basic() {
        System.out.println("========== 测试Ollama直接API调用 ==========");
        
        String text = "今天工作很开心";
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotion(text);
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        
        // 验证响应不为null
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        // 验证分数范围
        assertTrue(response.getScore() >= 1 && response.getScore() <= 100, 
                "分数应在1-100之间，实际: " + response.getScore());
        
        System.out.println("✅ 测试通过！");
    }
}
