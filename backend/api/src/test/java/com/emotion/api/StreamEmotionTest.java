package com.emotion.api;

import com.emotion.api.service.OllamaChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

/**
 * 流式情绪分析测试
 */
@SpringBootTest
public class StreamEmotionTest {

    @Autowired
    private OllamaChatService ollamaChatService;

    /**
     * 测试流式情绪分析
     */
    @Test
    public void testStreamEmotionAnalysis() throws InterruptedException {
        System.out.println("========== 测试流式情绪分析 ==========");
        
        String text = "今天天气真好，心情很愉快！";
        
        Flux<String> stream = ollamaChatService.analyzeEmotionStream(text);
        
        stream.doOnNext(chunk -> {
            System.out.print(chunk);  // 实时打印每个文本片段
        })
        .doOnError(error -> {
            System.err.println("\n错误: " + error.getMessage());
        })
        .doOnComplete(() -> {
            System.out.println("\n\n========== 流式输出完成 ==========");
        })
        .blockLast();  // 阻塞等待流完成
        
        // 等待一段时间以确保所有输出完成
        Thread.sleep(2000);
    }

    /**
     * 测试负面情绪的流式分析
     */
    @Test
    public void testStreamNegativeEmotion() throws InterruptedException {
        System.out.println("========== 测试负面情绪流式分析 ==========");
        
        String text = "今天工作很不顺利，被老板批评了，感觉很沮丧。";
        
        Flux<String> stream = ollamaChatService.analyzeEmotionStream(text);
        
        stream.doOnNext(chunk -> {
            System.out.print(chunk);
        })
        .doOnError(error -> {
            System.err.println("\n错误: " + error.getMessage());
        })
        .doOnComplete(() -> {
            System.out.println("\n\n========== 流式输出完成 ==========");
        })
        .blockLast();
        
        Thread.sleep(2000);
    }
}
