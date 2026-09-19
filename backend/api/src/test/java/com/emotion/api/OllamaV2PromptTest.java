package com.emotion.api;

import com.emotion.api.dto.EmotionAnalysisResponse;
import com.emotion.api.service.OllamaDirectService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ollama V2 Prompt 测试类
 * 测试优化后的情绪分析 prompt 效果
 */
@Slf4j
@SpringBootTest
public class OllamaV2PromptTest {

    @Autowired
    private OllamaDirectService ollamaDirectService;

    /**
     * 测试案例1：疲惫状态
     */
    @Test
    public void testTiredState() {
        String text = "今天正常上班，正常聊天，但回家后什么都不想做。";
        
        log.info("========== 测试案例1：疲惫状态 ==========");
        log.info("输入文本: {}", text);
        
        try {
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
            
            log.info("========== 分析结果 ==========");
            log.info("分数: {}", response.getScore());
            log.info("情绪标签: {}", response.getEmotion());
            log.info("建议: {}", response.getSuggestion());
            log.info("建议长度: {} 字", response.getSuggestion().length());
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    /**
     * 测试案例2：期待放假
     */
    @Test
    public void testHolidayExpectation() {
        String text = "明天终于放假了，好想睡到自然醒。";
        
        log.info("========== 测试案例2：期待放假 ==========");
        log.info("输入文本: {}", text);
        
        try {
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
            
            log.info("========== 分析结果 ==========");
            log.info("分数: {}", response.getScore());
            log.info("情绪标签: {}", response.getEmotion());
            log.info("建议: {}", response.getSuggestion());
            log.info("建议长度: {} 字", response.getSuggestion().length());
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    /**
     * 测试案例3：工作压力
     */
    @Test
    public void testWorkPressure() {
        String text = "最近项目压力好大，每天都在加班，感觉快撑不住了。";
        
        log.info("========== 测试案例3：工作压力 ==========");
        log.info("输入文本: {}", text);
        
        try {
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
            
            log.info("========== 分析结果 ==========");
            log.info("分数: {}", response.getScore());
            log.info("情绪标签: {}", response.getEmotion());
            log.info("建议: {}", response.getSuggestion());
            log.info("建议长度: {} 字", response.getSuggestion().length());
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    /**
     * 测试案例4：小确幸
     */
    @Test
    public void testSmallHappiness() {
        String text = "今天下班路上看到夕阳特别美，心情突然就好了。";
        
        log.info("========== 测试案例4：小确幸 ==========");
        log.info("输入文本: {}", text);
        
        try {
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
            
            log.info("========== 分析结果 ==========");
            log.info("分数: {}", response.getScore());
            log.info("情绪标签: {}", response.getEmotion());
            log.info("建议: {}", response.getSuggestion());
            log.info("建议长度: {} 字", response.getSuggestion().length());
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    /**
     * 测试案例5：社交疲惫
     */
    @Test
    public void testSocialFatigue() {
        String text = "周末参加了一个聚会，虽然玩得很开心，但现在只想一个人静静。";
        
        log.info("========== 测试案例5：社交疲惫 ==========");
        log.info("输入文本: {}", text);
        
        try {
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
            
            log.info("========== 分析结果 ==========");
            log.info("分数: {}", response.getScore());
            log.info("情绪标签: {}", response.getEmotion());
            log.info("建议: {}", response.getSuggestion());
            log.info("建议长度: {} 字", response.getSuggestion().length());
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    /**
     * 对比测试：V1 vs V2
     */
    @Test
    public void testCompareV1AndV2() {
        String text = "今天工作很不顺心，被领导批评了。";
        
        log.info("========== 对比测试：V1 vs V2 ==========");
        log.info("输入文本: {}\n", text);
        
        try {
            // V1 版本
            log.info("----- V1 版本（原版）-----");
            EmotionAnalysisResponse v1Response = ollamaDirectService.analyzeEmotion(text);
            log.info("分数: {}", v1Response.getScore());
            log.info("建议: {}", v1Response.getSuggestion());
            log.info("建议长度: {} 字\n", v1Response.getSuggestion().length());
            
            // V2 版本
            log.info("----- V2 版本（优化版）-----");
            EmotionAnalysisResponse v2Response = ollamaDirectService.analyzeEmotionV2(text);
            log.info("分数: {}", v2Response.getScore());
            log.info("情绪标签: {}", v2Response.getEmotion());
            log.info("建议: {}", v2Response.getSuggestion());
            log.info("建议长度: {} 字\n", v2Response.getSuggestion().length());
            
            log.info("=====================================\n");
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }
}
