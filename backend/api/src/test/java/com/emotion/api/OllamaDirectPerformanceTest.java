package com.emotion.api;

import com.emotion.api.dto.EmotionAnalysisResponse;
import com.emotion.api.service.OllamaDirectService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OllamaDirectService 性能测试 - 测试关闭思考模式的响应时间
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("prod")
public class OllamaDirectPerformanceTest {

    @Autowired
    private OllamaDirectService ollamaDirectService;

    /**
     * 测试单次请求的响应时间（关闭思考模式）
     */
    @Test
    public void testSingleRequest_Performance() {
        System.out.println("\n========== 测试单次请求性能（OllamaDirectService - 关闭思考模式） ==========");
        
        String text = "今天工作很开心，团队合作非常顺利";
        
        long startTime = System.currentTimeMillis();
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotion(text);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("输入文本: " + text);
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        System.out.println("⏱️  响应时间: " + elapsed + "ms (" + (elapsed / 1000.0) + "秒)");
        
        // 验证结果
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        assertTrue(response.getScore() >= 1 && response.getScore() <= 100, 
                "分数应在1-100范围内");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试多次请求的平均响应时间（关闭思考模式）
     */
    @Test
    public void testMultipleRequests_AveragePerformance() {
        System.out.println("\n========== 测试多次请求平均性能（OllamaDirectService - 关闭思考模式） ==========");
        
        int requestCount = 5;
        List<Long> responseTimes = new ArrayList<>();
        List<String> testTexts = List.of(
            "今天心情很好",
            "工作遇到了一些困难，感觉很沮丧",
            "和朋友一起出去玩很开心",
            "天气不错，适合散步",
            "今天完成了很多任务，很有成就感"
        );
        
        System.out.println("开始执行 " + requestCount + " 次请求...\n");
        
        for (int i = 0; i < requestCount; i++) {
            String text = testTexts.get(i % testTexts.size());
            
            long startTime = System.currentTimeMillis();
            EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotion(text);
            long elapsed = System.currentTimeMillis() - startTime;
            
            responseTimes.add(elapsed);
            
            System.out.println("请求 #" + (i + 1) + ":");
            System.out.println("  文本: " + text.substring(0, Math.min(20, text.length())) + "...");
            System.out.println("  分数: " + response.getScore());
            System.out.println("  ⏱️  耗时: " + elapsed + "ms (" + String.format("%.2f", elapsed / 1000.0) + "秒)");
            System.out.println();
        }
        
        // 计算统计数据
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double totalTime = responseTimes.stream().mapToLong(Long::longValue).sum();
        
        System.out.println("========== 性能统计 ==========");
        System.out.println("总请求数: " + requestCount);
        System.out.println("总耗时: " + String.format("%.2f", totalTime / 1000.0) + "秒");
        System.out.println("平均响应时间: " + String.format("%.2f", avgTime) + "ms (" + String.format("%.2f", avgTime / 1000.0) + "秒)");
        System.out.println("最快响应: " + minTime + "ms (" + String.format("%.2f", minTime / 1000.0) + "秒)");
        System.out.println("最慢响应: " + maxTime + "ms (" + String.format("%.2f", maxTime / 1000.0) + "秒)");
        System.out.println("==============================\n");
        
        // 验证所有响应都有效
        assertTrue(avgTime > 0, "平均响应时间应大于0");
        assertTrue(minTime > 0, "最小响应时间应大于0");
    }

    /**
     * 测试长文本的响应时间
     */
    @Test
    public void testLongText_Performance() {
        System.out.println("\n========== 测试长文本性能（OllamaDirectService - 关闭思考模式） ==========");
        
        String text = "今天完成了很多工作，虽然有点累，但是看到成果还是很开心的。" +
                "团队合作非常顺利，同事们都很支持我。早上开了一个重要的会议，" +
                "大家讨论了很多有价值的想法。下午处理了一些紧急的任务，" +
                "虽然压力有点大，但是最终都解决了。晚上打算好好休息一下，" +
                "明天继续努力！总的来说，今天是充实而有意义的一天。";
        
        System.out.println("文本长度: " + text.length() + " 字符");
        System.out.println("文本内容: " + text.substring(0, Math.min(50, text.length())) + "...\n");
        
        long startTime = System.currentTimeMillis();
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotion(text);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        System.out.println("⏱️  响应时间: " + elapsed + "ms (" + String.format("%.2f", elapsed / 1000.0) + "秒)");
        
        // 验证结果
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }

    /**
     * 测试短文本的响应时间
     */
    @Test
    public void testShortText_Performance() {
        System.out.println("\n========== 测试短文本性能（OllamaDirectService - 关闭思考模式） ==========");
        
        String text = "开心";
        
        System.out.println("文本长度: " + text.length() + " 字符");
        System.out.println("文本内容: " + text + "\n");
        
        long startTime = System.currentTimeMillis();
        EmotionAnalysisResponse response = ollamaDirectService.analyzeEmotion(text);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("情绪分数: " + response.getScore());
        System.out.println("建议: " + response.getSuggestion());
        System.out.println("⏱️  响应时间: " + elapsed + "ms (" + String.format("%.2f", elapsed / 1000.0) + "秒)");
        
        // 验证结果
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getScore(), "分数不应为null");
        assertNotNull(response.getSuggestion(), "建议不应为null");
        
        System.out.println("✅ 测试通过");
        System.out.println();
    }
}
