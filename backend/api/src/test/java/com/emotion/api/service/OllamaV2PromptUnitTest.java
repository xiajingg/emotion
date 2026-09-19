package com.emotion.api.service;

import com.emotion.api.dto.EmotionAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ollama V2 Prompt 单元测试
 * 测试优化后的情绪分析 prompt 输出质量
 */
@Slf4j
@SpringBootTest
public class OllamaV2PromptUnitTest {

    @Autowired
    private OllamaDirectService ollamaDirectService;

    /**
     * 测试案例1：疲惫状态 - 验证 emotion 字段和 suggestion 长度
     */
    @Test
    @DisplayName("测试疲惫状态 - 应返回情绪透支标签和80-150字建议")
    public void testTiredState_ShouldReturnEmotionLabelAndProperLength() {
        String text = "今天正常上班，正常聊天，但回家后什么都不想做。";
        
        log.info("\n========== 测试案例1：疲惫状态 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getScore(), "分数不应为空");
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        assertNotNull(response.getSuggestion(), "建议不应为空");
        
        // 验证分数范围
        assertTrue(response.getScore() >= 1 && response.getScore() <= 100, 
                "分数应在1-100之间，实际: " + response.getScore());
        
        // 验证情绪标签不为空且不是普通词
        assertFalse(response.getEmotion().isEmpty(), "情绪标签不应为空");
        assertNotEquals("开心", response.getEmotion(), "情绪标签不应是普通词'开心'");
        assertNotEquals("难过", response.getEmotion(), "情绪标签不应是普通词'难过'");
        
        // 验证建议长度（80-150字）
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        // 验证建议不包含 AI 味重的表达
        assertFalse(response.getSuggestion().contains("根据你的描述"), 
                "建议不应包含'根据你的描述'");
        assertFalse(response.getSuggestion().contains("建议调整心态"), 
                "建议不应包含'建议调整心态'");
        assertFalse(response.getSuggestion().contains("请保持积极"), 
                "建议不应包含'请保持积极'");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 测试案例2：期待放假 - 验证正面情绪的识别
     */
    @Test
    @DisplayName("测试期待放假 - 应返回高分和正面情绪标签")
    public void testHolidayExpectation_ShouldReturnHighScoreAndPositiveEmotion() {
        String text = "明天终于放假了，好想睡到自然醒。";
        
        log.info("\n========== 测试案例2：期待放假 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertTrue(response.getScore() >= 60, 
                "正面情绪分数应 >= 60，实际: " + response.getScore());
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        
        // 验证建议长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 测试案例3：工作压力 - 验证负面情绪的识别
     */
    @Test
    @DisplayName("测试工作压力 - 应返回低分和负面情绪标签")
    public void testWorkPressure_ShouldReturnLowScoreAndNegativeEmotion() {
        String text = "最近项目压力好大，每天都在加班，感觉快撑不住了。";
        
        log.info("\n========== 测试案例3：工作压力 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertTrue(response.getScore() <= 40, 
                "负面情绪分数应 <= 40，实际: " + response.getScore());
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        
        // 验证建议长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 测试案例4：小确幸 - 验证中等正面情绪
     */
    @Test
    @DisplayName("测试小确幸 - 应返回中等偏上分数")
    public void testSmallHappiness_ShouldReturnMediumHighScore() {
        String text = "今天下班路上看到夕阳特别美，心情突然就好了。";
        
        log.info("\n========== 测试案例4：小确幸 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertTrue(response.getScore() >= 50 && response.getScore() <= 80, 
                "小确幸分数应在50-80之间，实际: " + response.getScore());
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        
        // 验证建议长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 测试案例5：社交疲惫 - 验证复杂情绪的识别
     */
    @Test
    @DisplayName("测试社交疲惫 - 应识别复杂情绪状态")
    public void testSocialFatigue_ShouldRecognizeComplexEmotion() {
        String text = "周末参加了一个聚会，虽然玩得很开心，但现在只想一个人静静。";
        
        log.info("\n========== 测试案例5：社交疲惫 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        
        // 验证情绪标签有画面感（不是简单的"开心"或"累"）
        String emotion = response.getEmotion();
        assertFalse(emotion.equals("开心") || emotion.equals("累"), 
                "情绪标签应有画面感，不应是简单词汇，实际: " + emotion);
        
        // 验证建议长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 对比测试：V1 vs V2 - 验证 V2 的优势
     */
    @Test
    @DisplayName("对比测试 V1 vs V2 - V2 应包含 emotion 字段且建议更长")
    public void testCompareV1AndV2_V2ShouldHaveEmotionAndLongerSuggestion() {
        String text = "今天工作很不顺心，被领导批评了。";
        
        log.info("\n========== 对比测试：V1 vs V2 ==========");
        log.info("输入文本: {}\n", text);
        
        // V1 版本
        EmotionAnalysisResponse v1Response = ollamaDirectService.analyzeEmotion(text);
        log.info("----- V1 版本（原版）-----");
        log.info("分数: {}", v1Response.getScore());
        log.info("情绪标签: {}", v1Response.getEmotion()); // V1 可能为 null
        log.info("建议: {}", v1Response.getSuggestion());
        int v1Length = v1Response.getSuggestion().length();
        log.info("建议长度: {} 字\n", v1Length);
        
        // V2 版本
        EmotionAnalysisResponse v2Response = ollamaDirectService.analyzeEmotionV2(text);
        log.info("----- V2 版本（优化版）-----");
        log.info("分数: {}", v2Response.getScore());
        log.info("情绪标签: {}", v2Response.getEmotion());
        log.info("建议: {}", v2Response.getSuggestion());
        int v2Length = v2Response.getSuggestion().length();
        log.info("建议长度: {} 字\n", v2Length);
        
        // 验证 V2 的优势
        assertNotNull(v2Response.getEmotion(), "V2 必须包含 emotion 字段");
        assertTrue(v2Length >= 80, "V2 建议长度应 >= 80字，实际: " + v2Length);
        
        // V2 的建议应该比 V1 长（或至少一样长）
        assertTrue(v2Length >= v1Length, 
                "V2 建议长度应 >= V1，V1: " + v1Length + ", V2: " + v2Length);
        
        log.info("=====================================\n");
        log.info("✅ V2 优势验证通过：");
        log.info("   - V2 包含 emotion 字段: {}", v2Response.getEmotion());
        log.info("   - V2 建议长度: {} 字 (V1: {} 字)", v2Length, v1Length);
    }

    /**
     * 边界测试：极短文本
     */
    @Test
    @DisplayName("测试极短文本 - 应能正确处理")
    public void testVeryShortText_ShouldHandleCorrectly() {
        String text = "好累。";
        
        log.info("\n========== 边界测试：极短文本 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getScore(), "分数不应为空");
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        assertNotNull(response.getSuggestion(), "建议不应为空");
        
        // 即使是短文本，建议也应该有一定长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 50, 
                "即使是短文本，建议也应 >= 50字，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }

    /**
     * 边界测试：较长文本
     */
    @Test
    @DisplayName("测试较长文本 - 应能正确处理")
    public void testLongText_ShouldHandleCorrectly() {
        String text = "最近工作压力特别大，每天都要加班到很晚，回到家已经精疲力尽了。周末本来想好好休息一下，但是又有很多事情要做，感觉完全没有自己的时间。有时候真的觉得很累，不知道这样的日子什么时候才能结束。";
        
        log.info("\n========== 边界测试：较长文本 ==========");
        log.info("输入文本: {}", text);
        
        // 执行测试
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotionV2(text);
        
        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertTrue(response.getScore() <= 40, 
                "长期疲惫状态分数应 <= 40，实际: " + response.getScore());
        assertNotNull(response.getEmotion(), "情绪标签不应为空");
        
        // 验证建议长度
        int suggestionLength = response.getSuggestion().length();
        assertTrue(suggestionLength >= 80 && suggestionLength <= 150, 
                "建议长度应在80-150字之间，实际: " + suggestionLength + "字");
        
        log.info("✅ 分数: {}", response.getScore());
        log.info("✅ 情绪标签: {}", response.getEmotion());
        log.info("✅ 建议: {}", response.getSuggestion());
        log.info("✅ 建议长度: {} 字", suggestionLength);
        log.info("=====================================\n");
    }
}
