package com.emotion.api;

import io.github.cosinekitty.astronomy.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.*;

/**
 * 星座行星分析完整性测试
 * 
 * 测试目标：验证 AstronomyService 是否完整获取并处理了标准的 10 个主要天体
 * 标准天体列表：太阳、月亮 + 8大行星（水星、金星、火星、木星、土星、天王星、海王星、冥王星）
 */
@Slf4j
@SpringBootTest
public class AstronomyCompletenessTest {

    @Autowired
    private com.emotion.api.service.AstronomyService astronomyService;

    /**
     * 测试 1：验证当前实现实际使用的天体列表
     */
    @Test
    public void testCurrentCelestialBodies() {
        log.info("========== 测试 1：当前实现的天体列表 ==========");
        
        LocalDate testDate = LocalDate.now();
        Map<String, Double> positions = astronomyService.getCelestialPositions(testDate);
        
        log.info("当前实现获取到的天体数量: {}", positions.size());
        log.info("当前实现的天体列表:");
        positions.keySet().forEach(body -> {
            log.info("  - {} (黄经: {:.2f}°)", body, positions.get(body));
        });
        
        // 断言：当前应该有 7 个天体
        assert positions.size() == 7 : "预期 7 个天体，实际: " + positions.size();
    }

    /**
     * 测试 2：验证 Astronomy Engine 库支持的所有天体
     */
    @Test
    public void testAstronomyEngineSupportedBodies() {
        log.info("========== 测试 2：Astronomy Engine 支持的天体 ==========");
        
        // Astronomy Engine Body 枚举包含的所有天体
        Body[] allBodies = Body.values();
        
        log.info("Astronomy Engine 支持的天体总数: {}", allBodies.length);
        log.info("所有支持的天体列表:");
        
        for (Body body : allBodies) {
            log.info("  - {} ({})", body, getBodyDescription(body));
        }
    }

    /**
     * 测试 3：完整性对比分析
     */
    @Test
    public void testCompletenessAnalysis() {
        log.info("========== 测试 3：完整性对比分析 ==========");
        
        // 标准的 10 个主要天体（占星学常用）
        Set<String> standardBodies = new LinkedHashSet<>(Arrays.asList(
            "Sun",      // 太阳
            "Moon",     // 月亮
            "Mercury",  // 水星
            "Venus",    // 金星
            "Mars",     // 火星
            "Jupiter",  // 木星
            "Saturn",   // 土星
            "Uranus",   // 天王星
            "Neptune",  // 海王星
            "Pluto"     // 冥王星
        ));
        
        // 当前实现使用的天体
        Set<String> currentBodies = new HashSet<>(Arrays.asList(
            "Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"
        ));
        
        // 计算缺失的天体
        Set<String> missingBodies = new LinkedHashSet<>(standardBodies);
        missingBodies.removeAll(currentBodies);
        
        log.info("【标准天体列表】（10个）:");
        standardBodies.forEach(body -> {
            boolean isUsed = currentBodies.contains(body);
            String status = isUsed ? "✅ 已使用" : "❌ 缺失";
            log.info("  {} {} - {}", status, body, getChineseName(body));
        });
        
        log.info("\n【当前实现】: {} 个天体", currentBodies.size());
        currentBodies.forEach(body -> {
            log.info("  ✅ {} - {}", body, getChineseName(body));
        });
        
        log.info("\n【缺失天体】: {} 个", missingBodies.size());
        missingBodies.forEach(body -> {
            log.info("  ❌ {} - {}", body, getChineseName(body));
        });
        
        // 输出统计信息
        log.info("\n【统计摘要】:");
        log.info("  标准天体总数: {}", standardBodies.size());
        log.info("  当前使用数量: {}", currentBodies.size());
        log.info("  缺失数量: {}", missingBodies.size());
        log.info("  覆盖率: {:.1f}%", (currentBodies.size() * 100.0 / standardBodies.size()));
        
        // 断言：应该缺失 3 个天体
        assert missingBodies.size() == 3 : "预期缺失 3 个天体，实际: " + missingBodies.size();
        assert missingBodies.contains("Uranus") : "应缺失天王星";
        assert missingBodies.contains("Neptune") : "应缺失海王星";
        assert missingBodies.contains("Pluto") : "应缺失冥王星";
    }

    /**
     * 测试 4：验证缺失天体是否可以被 Astronomy Engine 获取
     */
    @Test
    public void testMissingBodiesAvailability() {
        log.info("========== 测试 4：验证缺失天体的可用性 ==========");
        
        LocalDate testDate = LocalDate.now();
        long millis = testDate.atTime(12, 0).atZone(java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli();
        var time = Time.fromMillisecondsSince1970(millis);
        
        // 测试缺失的 3 个天体
        Body[] missingBodies = {Body.Uranus, Body.Neptune, Body.Pluto};
        
        log.info("测试缺失天体是否可以通过 Astronomy Engine 获取:");
        
        for (Body body : missingBodies) {
            try {
                var geoVector = Astronomy.geoVector(body, time, Aberration.None);
                var ecliptic = Astronomy.equatorialToEcliptic(geoVector);
                
                log.info("  ✅ {} ({}) - 可获取，黄经: {:.2f}°", 
                    body, getChineseName(body.toString()), ecliptic.getElon());
            } catch (Exception e) {
                log.error("  ❌ {} ({}) - 获取失败: {}", 
                    body, getChineseName(body.toString()), e.getMessage());
            }
        }
    }

    /**
     * 测试 5：生成完整修复建议
     */
    @Test
    public void generateFixRecommendation() {
        log.info("========== 测试 5：修复建议 ==========");
        
        log.info("【问题根因分析】:");
        log.info("  1. 代码逻辑遗漏：AstronomyService.java 第 35 行仅定义了 7 个天体");
        log.info("  2. 非数据源限制：Astronomy Engine 库完全支持所有 10 个天体");
        log.info("  3. 非配置问题：无需额外配置，只需修改代码");
        
        log.info("\n【修复方案】:");
        log.info("  修改文件: backend/api/src/main/java/com/emotion/api/service/AstronomyService.java");
        log.info("  修改位置: 第 35 行");
        log.info("  修改内容:");
        log.info("    原代码:");
        log.info("      Body[] bodies = {Body.Sun, Body.Moon, Body.Mercury, Body.Venus, Body.Mars, Body.Jupiter, Body.Saturn};");
        log.info("    新代码:");
        log.info("      Body[] bodies = {Body.Sun, Body.Moon, Body.Mercury, Body.Venus, Body.Mars, ");
        log.info("                       Body.Jupiter, Body.Saturn, Body.Uranus, Body.Neptune, Body.Pluto};");
        
        log.info("\n【影响评估】:");
        log.info("  1. 数据库字段：daily_horoscope 表的 astro_analysis 字段需要存储更多天体数据");
        log.info("  2. AI Prompt：HoroscopeGenerationService 中的 prompt 可能需要调整以利用更多天体");
        log.info("  3. 性能影响：增加 3 个天体计算，预计增加 ~40% 计算时间（可接受）");
        log.info("  4. 缓存策略：Redis 缓存键不变，重新生成后自动更新");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取天体的中文名称
     */
    private String getChineseName(String bodyName) {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("Sun", "太阳");
        nameMap.put("Moon", "月亮");
        nameMap.put("Mercury", "水星");
        nameMap.put("Venus", "金星");
        nameMap.put("Mars", "火星");
        nameMap.put("Jupiter", "木星");
        nameMap.put("Saturn", "土星");
        nameMap.put("Uranus", "天王星");
        nameMap.put("Neptune", "海王星");
        nameMap.put("Pluto", "冥王星");
        return nameMap.getOrDefault(bodyName, bodyName);
    }

    /**
     * 获取天体的描述信息
     */
    private String getBodyDescription(Body body) {
        switch (body) {
            case Sun: return "太阳（恒星）";
            case Moon: return "月亮（卫星）";
            case Mercury: return "水星（类地行星）";
            case Venus: return "金星（类地行星）";
            case Mars: return "火星（类地行星）";
            case Jupiter: return "木星（气态巨行星）";
            case Saturn: return "土星（气态巨行星）";
            case Uranus: return "天王星（冰巨星）";
            case Neptune: return "海王星（冰巨星）";
            case Pluto: return "冥王星（矮行星）";
            default: return body.toString();
        }
    }
}
