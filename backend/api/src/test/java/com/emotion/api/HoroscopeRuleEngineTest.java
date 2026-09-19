package com.emotion.api;

import com.emotion.api.service.AstronomyService;
import com.emotion.api.service.HoroscopeRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;

/**
 * 星座运势规则引擎测试 - 验证不同星座的内容差异化
 */
@Slf4j
@SpringBootTest
public class HoroscopeRuleEngineTest {

    @Autowired
    private AstronomyService astronomyService;
    
    @Autowired
    private HoroscopeRuleEngine ruleEngine;

    /**
     * 测试：验证2026-05-13这天，不同星座的content是否有明显差异
     */
    @Test
    public void testContentDifferentiation() {
        LocalDate testDate = LocalDate.of(2026, 5, 13);
        
        log.info("========== 测试星座运势内容差异化 ==========");
        log.info("测试日期: {}", testDate);
        
        // 获取天文数据
        Map<String, Double> positions = astronomyService.getCelestialPositions(testDate);
        
        log.info("\n【当天星象数据】");
        log.info("太阳: {:.2f}° ({})", positions.get("Sun"), getZodiacByDegree(positions.get("Sun")));
        log.info("月亮: {:.2f}° ({})", positions.get("Moon"), getZodiacByDegree(positions.get("Moon")));
        log.info("金星: {:.2f}° ({})", positions.get("Venus"), getZodiacByDegree(positions.get("Venus")));
        log.info("火星: {:.2f}° ({})", positions.get("Mars"), getZodiacByDegree(positions.get("Mars")));
        log.info("木星: {:.2f}° ({})", positions.get("Jupiter"), getZodiacByDegree(positions.get("Jupiter")));
        log.info("土星: {:.2f}° ({})", positions.get("Saturn"), getZodiacByDegree(positions.get("Saturn")));
        
        String[] zodiacSigns = {"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", 
                                "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"};
        
        log.info("\n\n========== 各星座今日指引对比 ==========\n");
        
        for (String sign : zodiacSigns) {
            Map<String, String> data = ruleEngine.generateHoroscopeData(positions, sign);
            String content = data.get("content");
            
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("【{}】", getZodiacChineseName(sign));
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info(content);
            log.info("");
        }
        
        log.info("========== 测试结束 ==========");
    }
    
    private String getZodiacByDegree(double degree) {
        String[] signs = {"白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", 
                          "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"};
        int index = (int) (degree / 30) % 12;
        return signs[index];
    }
    
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
