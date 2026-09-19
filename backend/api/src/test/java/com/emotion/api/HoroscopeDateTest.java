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
import java.util.List;

/**
 * 2026-05-10 星座运势生成测试
 * 
 * 测试目标：
 * 1. 生成指定日期的所有星座运势
 * 2. 验证 10 个天体数据是否正确获取
 * 3. 查看完整的运势内容（星象解码、AI建议、宜忌、分数）
 */
@Slf4j
@SpringBootTest
public class HoroscopeDateTest {

    @Autowired
    private HoroscopeGenerationService generationService;
    
    @Autowired
    private DailyHoroscopeMapper horoscopeMapper;

    /**
     * 生成并查看 2026-05-13 的所有星座运势
     */
    @Test
    public void testGenerateHoroscopeForSpecificDate() {
        LocalDate targetDate = LocalDate.of(2026, 5, 13);
        
        log.info("========== 开始生成 {} 的星座运势 ==========", targetDate);
        
        // 1. 生成运势
        generationService.generateDailyHoroscopes(targetDate);
        
        // 2. 查询生成的所有星座运势
        LambdaQueryWrapper<DailyHoroscope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyHoroscope::getDate, targetDate)
               .orderByAsc(DailyHoroscope::getZodiacSign);
        
        List<DailyHoroscope> horoscopes = horoscopeMapper.selectList(wrapper);
        
        log.info("\n\n========== {} 星座运势完整报告 ==========", targetDate);
        log.info("共生成 {} 个星座的运势\n", horoscopes.size());
        
        // 3. 逐个打印每个星座的详细信息
        for (int i = 0; i < horoscopes.size(); i++) {
            DailyHoroscope h = horoscopes.get(i);
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("【{}/{}】{}", i + 1, horoscopes.size(), getZodiacChineseName(h.getZodiacSign()));
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // 星象解码（原始天文数据）
            log.info("\n🔮 星象解码:");
            log.info("   {}", h.getAstroAnalysis());
            
            // 今日指引（规则引擎解读）
            log.info("\n💫 今日指引:");
            log.info("   {}", h.getContent());
            
            // AI 暖心建议
            log.info("\n💝 AI 暖心建议:");
            log.info("   {}", h.getAiAdvice());
            
            // 今日宜忌
            log.info("\n📋 今日宜忌:");
            log.info("   {}", h.getDosAndDonts());
            
            // 三维运势评分
            log.info("\n📊 三维运势评分:");
            log.info("   ❤️ 情感运势: {}", h.getLoveFortune());
            log.info("   💰 财富运势: {}", h.getWealthFortune());
            log.info("   💼 事业运势: {}", h.getCareerFortune());
            
            // 技术信息
            log.info("\n⚙️ 技术信息:");
            log.info("   ID: {}", h.getId());
            log.info("   AI 模型: {}", h.getAiModel());
            log.info("   创建时间: {}", h.getCreateTime());
            
            log.info(""); // 空行分隔
        }
        
        log.info("========== 报告结束 ==========");
    }
    
    /**
     * 单独查看某个星座的运势（例如白羊座）
     */
    @Test
    public void testViewSingleZodiacHoroscope() {
        LocalDate targetDate = LocalDate.of(2026, 5, 10);
        String zodiacSign = "Aries"; // 白羊座
        
        log.info("========== 查看 {} {} 的运势 ==========", targetDate, getZodiacChineseName(zodiacSign));
        
        // 先确保已生成
        generationService.generateDailyHoroscopes(targetDate);
        
        // 查询指定星座
        LambdaQueryWrapper<DailyHoroscope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyHoroscope::getDate, targetDate)
               .eq(DailyHoroscope::getZodiacSign, zodiacSign);
        
        DailyHoroscope horoscope = horoscopeMapper.selectOne(wrapper);
        
        if (horoscope == null) {
            log.error("未找到 {} {} 的运势数据", targetDate, getZodiacChineseName(zodiacSign));
            return;
        }
        
        // 打印详细信息
        log.info("\n日期: {}", horoscope.getDate());
        log.info("星座: {} ({})", getZodiacChineseName(horoscope.getZodiacSign()), horoscope.getZodiacSign());
        log.info("\n【星象解码】");
        log.info(horoscope.getAstroAnalysis());
        
        log.info("\n【今日指引】");
        log.info(horoscope.getContent());
        
        log.info("\n【AI 暖心建议】");
        log.info(horoscope.getAiAdvice());
        
        log.info("\n【今日宜忌】");
        log.info(horoscope.getDosAndDonts());
        
        log.info("\n【三维运势评分】");
        log.info("  情感: {}", horoscope.getLoveFortune());
        log.info("  财富: {}", horoscope.getWealthFortune());
        log.info("  事业: {}", horoscope.getCareerFortune());
        
        log.info("\n========== 查看结束 ==========");
    }
    
    /**
     * 获取星座中文名称
     */
    private String getZodiacChineseName(String sign) {
        return switch (sign) {
            case "Aries" -> "白羊座";
            case "Taurus" -> "金牛座";
            case "Gemini" -> "双子座";
            case "Cancer" -> "巨蟹座";
            case "Leo" -> "狮子座";
            case "Virgo" -> "处女座";
            case "Libra" -> "天秤座";
            case "Scorpio" -> "天蝎座";
            case "Sagittarius" -> "射手座";
            case "Capricorn" -> "摩羯座";
            case "Aquarius" -> "水瓶座";
            case "Pisces" -> "双鱼座";
            default -> sign;
        };
    }
}
