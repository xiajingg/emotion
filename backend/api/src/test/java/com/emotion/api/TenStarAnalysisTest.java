package com.emotion.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.mapper.DailyHoroscopeMapper;
import com.emotion.api.repository.po.DailyHoroscope;
import com.emotion.api.service.HoroscopeGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

/**
 * 验证10星分析优化效果
 */
@Slf4j
@SpringBootTest
public class TenStarAnalysisTest {

    @Autowired
    private HoroscopeGenerationService generationService;
    
    @Autowired
    private DailyHoroscopeMapper horoscopeMapper;

    /**
     * 生成并查看优化后的星象解码（使用2026-05-11避免缓存）
     */
    @Test
    public void testTenStarAnalysis() {
        LocalDate targetDate = LocalDate.of(2026, 5, 11);
        
        log.info("========== 开始生成 {} 的星座运势（10星完整版） ==========", targetDate);
        
        // 1. 生成运势
        generationService.generateDailyHoroscopes(targetDate);
        
        // 2. 查询白羊座的运势
        LambdaQueryWrapper<DailyHoroscope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyHoroscope::getDate, targetDate)
               .eq(DailyHoroscope::getZodiacSign, "Aries");
        
        DailyHoroscope horoscope = horoscopeMapper.selectOne(wrapper);
        
        if (horoscope == null) {
            log.error("未找到运势数据");
            return;
        }
        
        log.info("\n\n========== 白羊座 {} 运势（10星完整版） ==========", targetDate);
        
        // 打印完整的星象解码
        log.info("\n【星象解码 - 完整天文数据】\n");
        log.info(horoscope.getAstroAnalysis());
        
        log.info("\n【今日指引 - 规则引擎解读】\n");
        log.info(horoscope.getContent());
        
        log.info("\n【AI 暖心建议】\n");
        log.info(horoscope.getAiAdvice());
        
        log.info("\n【今日宜忌】\n");
        log.info(horoscope.getDosAndDonts());
        
        log.info("\n【三维运势评分】");
        log.info("  情感运势: {}", horoscope.getLoveFortune());
        log.info("  财富运势: {}", horoscope.getWealthFortune());
        log.info("  事业运势: {}", horoscope.getCareerFortune());
        
        log.info("\n========== 验证结束 ==========");
        
        // 验证是否包含多个天体
        String astroAnalysis = horoscope.getAstroAnalysis();
        boolean hasMercury = astroAnalysis.contains("水星");
        boolean hasMars = astroAnalysis.contains("火星");
        boolean hasJupiter = astroAnalysis.contains("木星");
        boolean hasSaturn = astroAnalysis.contains("土星");
        boolean hasUranus = astroAnalysis.contains("天王星");
        boolean hasNeptune = astroAnalysis.contains("海王星");
        boolean hasPluto = astroAnalysis.contains("冥王星");
        
        log.info("\n天体完整性检查:");
        log.info("  水星: {}", hasMercury ? "YES" : "NO");
        log.info("  火星: {}", hasMars ? "YES" : "NO");
        log.info("  木星: {}", hasJupiter ? "YES" : "NO");
        log.info("  土星: {}", hasSaturn ? "YES" : "NO");
        log.info("  天王星: {}", hasUranus ? "YES" : "NO");
        log.info("  海王星: {}", hasNeptune ? "YES" : "NO");
        log.info("  冥王星: {}", hasPluto ? "YES" : "NO");
        
        int count = 0;
        if (hasMercury) count++;
        if (hasMars) count++;
        if (hasJupiter) count++;
        if (hasSaturn) count++;
        if (hasUranus) count++;
        if (hasNeptune) count++;
        if (hasPluto) count++;
        
        log.info("\n统计: 7个额外天体中显示了 {}/7 个", count);
        
        if (count >= 5) {
            log.info("SUCCESS: 优化成功！星象解码已包含大部分行星数据");
        } else {
            log.warn("WARNING: 优化未完全生效，仅显示 {}/7 个额外天体", count);
        }
    }
}
