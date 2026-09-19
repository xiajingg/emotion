package com.emotion.api;

import com.emotion.api.service.HoroscopeGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

/**
 * 快速测试 AI 返回内容
 */
@SpringBootTest
public class QuickAiTest {

    @Autowired
    private HoroscopeGenerationService generationService;

    @Test
    public void testQuickGenerate() {
        // 只生成今天的一个星座
        LocalDate today = LocalDate.now();
        generationService.generateDailyHoroscopes(today);
        System.out.println("完成！请查看日志中的 AI 原始返回内容");
    }
}
