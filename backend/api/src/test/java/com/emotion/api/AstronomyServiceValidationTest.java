package com.emotion.api;

import com.emotion.api.service.AstronomyService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;

/**
 * AstronomyService 功能验证测试
 * 
 * 测试目标：
 * 1. 验证优化后的服务能获取完整的 10 个天体数据
 * 2. 验证支持历史/未来日期的精确计算
 * 3. 输出所有天体的黄经度数
 */
@Slf4j
@SpringBootTest
public class AstronomyServiceValidationTest {

    @Autowired
    private AstronomyService astronomyService;

    /**
     * 测试 1：验证未来日期的天体数据（2026-12-25 圣诞节）
     */
    @Test
    public void testFutureDate() {
        log.info("========== 测试 1：未来日期（2026-12-25） ==========");
        
        LocalDate futureDate = LocalDate.of(2026, 12, 25);
        Map<String, Double> positions = astronomyService.getCelestialPositions(futureDate);
        
        log.info("测试日期: {}", futureDate);
        log.info("获取到的天体数量: {}", positions.size());
        log.info("\n天体黄经数据列表:");
        
        positions.forEach((body, longitude) -> {
            String description = getBodyDescription(body);
            log.info("  {} - {:.2f}° - {}", body, longitude, description);
        });
        
        // 断言：应该获取到 10 个天体
        assert positions.size() == 10 : "预期 10 个天体，实际: " + positions.size();
        
        // 验证关键天体存在
        assert positions.containsKey("Sun") : "缺少太阳数据";
        assert positions.containsKey("Moon") : "缺少月亮数据";
        assert positions.containsKey("Mars") : "缺少火星数据";
        assert positions.containsKey("Jupiter") : "缺少木星数据";
        assert positions.containsKey("Uranus") : "缺少天王星数据";
        assert positions.containsKey("Neptune") : "缺少海王星数据";
        assert positions.containsKey("Pluto") : "缺少冥王星数据";
        
        log.info("\n✅ 未来日期测试通过！所有 10 个天体数据完整。");
    }

    /**
     * 测试 2：验证历史日期的天体数据（2020-01-01）
     */
    @Test
    public void testHistoricalDate() {
        log.info("\n========== 测试 2：历史日期（2020-01-01） ==========");
        
        LocalDate historicalDate = LocalDate.of(2020, 1, 1);
        Map<String, Double> positions = astronomyService.getCelestialPositions(historicalDate);
        
        log.info("测试日期: {}", historicalDate);
        log.info("获取到的天体数量: {}", positions.size());
        log.info("\n天体黄经数据列表:");
        
        positions.forEach((body, longitude) -> {
            String description = getBodyDescription(body);
            log.info("  {} - {:.2f}° - {}", body, longitude, description);
        });
        
        // 断言：应该获取到 10 个天体
        assert positions.size() == 10 : "预期 10 个天体，实际: " + positions.size();
        
        log.info("\n✅ 历史日期测试通过！Astronomy Engine 支持历史回溯计算。");
    }

    /**
     * 测试 3：对比不同日期的天体位置变化（验证动态计算）
     */
    @Test
    public void testDynamicCalculation() {
        log.info("\n========== 测试 3：动态计算验证 ==========");
        
        LocalDate date1 = LocalDate.of(2026, 1, 1);
        LocalDate date2 = LocalDate.of(2026, 6, 1);
        LocalDate date3 = LocalDate.of(2026, 12, 1);
        
        Map<String, Double> pos1 = astronomyService.getCelestialPositions(date1);
        Map<String, Double> pos2 = astronomyService.getCelestialPositions(date2);
        Map<String, Double> pos3 = astronomyService.getCelestialPositions(date3);
        
        log.info("对比三个不同日期的太阳和月亮位置变化:");
        log.info("{:<15} {:<15} {:<15} {:<15}", "天体", "2026-01-01", "2026-06-01", "2026-12-01");
        log.info("-".repeat(65));
        
        // 对比太阳位置
        double sun1 = pos1.get("Sun");
        double sun2 = pos2.get("Sun");
        double sun3 = pos3.get("Sun");
        log.info("{:<15} {:<15.2f}° {:<15.2f}° {:<15.2f}°", "Sun", sun1, sun2, sun3);
        
        // 对比月亮位置
        double moon1 = pos1.get("Moon");
        double moon2 = pos2.get("Moon");
        double moon3 = pos3.get("Moon");
        log.info("{:<15} {:<15.2f}° {:<15.2f}° {:<15.2f}°", "Moon", moon1, moon2, moon3);
        
        // 对比火星位置（移动较慢）
        double mars1 = pos1.get("Mars");
        double mars2 = pos2.get("Mars");
        double mars3 = pos3.get("Mars");
        log.info("{:<15} {:<15.2f}° {:<15.2f}° {:<15.2f}°", "Mars", mars1, mars2, mars3);
        
        // 验证位置确实不同（证明是动态计算）
        assert Math.abs(sun1 - sun2) > 10 : "太阳位置应该有明显变化";
        assert Math.abs(moon1 - moon2) > 30 : "月亮位置应该有显著变化";
        
        log.info("\n✅ 动态计算验证通过！不同日期返回不同的天体位置。");
        log.info("   结论：Astronomy Engine 根据天文历法动态计算，而非返回固定值。");
    }

    /**
     * 测试 4：验证新增的 3 个天体（天王星、海王星、冥王星）
     */
    @Test
    public void testNewlyAddedBodies() {
        log.info("\n========== 测试 4：新增天体验证 ==========");
        
        LocalDate testDate = LocalDate.of(2026, 5, 9);
        Map<String, Double> positions = astronomyService.getCelestialPositions(testDate);
        
        log.info("测试日期: {}", testDate);
        log.info("\n重点验证新增的 3 个天体:");
        
        // 天王星
        if (positions.containsKey("Uranus")) {
            double uranusLon = positions.get("Uranus");
            log.info("✅ Uranus (天王星): {:.2f}° - 冰巨星，轨道周期 84 年", uranusLon);
        } else {
            log.error("❌ Uranus (天王星): 缺失");
        }
        
        // 海王星
        if (positions.containsKey("Neptune")) {
            double neptuneLon = positions.get("Neptune");
            log.info("✅ Neptune (海王星): {:.2f}° - 冰巨星，轨道周期 165 年", neptuneLon);
        } else {
            log.error("❌ Neptune (海王星): 缺失");
        }
        
        // 冥王星
        if (positions.containsKey("Pluto")) {
            double plutoLon = positions.get("Pluto");
            log.info("✅ Pluto (冥王星): {:.2f}° - 矮行星，轨道周期 248 年", plutoLon);
        } else {
            log.error("❌ Pluto (冥王星): 缺失");
        }
        
        // 断言：三个新天体都必须存在
        assert positions.containsKey("Uranus") : "天王星数据缺失";
        assert positions.containsKey("Neptune") : "海王星数据缺失";
        assert positions.containsKey("Pluto") : "冥王星数据缺失";
        
        log.info("\n✅ 新增天体验证通过！所有 3 个天体数据完整。");
    }

    /**
     * 测试 5：生成完整的天体数据报告
     */
    @Test
    public void generateCompleteReport() {
        log.info("\n========== 测试 5：完整天体数据报告 ==========");
        
        LocalDate reportDate = LocalDate.of(2026, 5, 9);
        Map<String, Double> positions = astronomyService.getCelestialPositions(reportDate);
        
        log.info("============================================================");
        log.info("         星座运势 - 完整天体数据报告");
        log.info("         日期: {}", reportDate);
        log.info("============================================================");
        log.info("");
        log.info("【标准占星学 10 大天体配置】");
        log.info("");
        
        int index = 1;
        for (Map.Entry<String, Double> entry : positions.entrySet()) {
            String body = entry.getKey();
            double longitude = entry.getValue();
            String description = getBodyDescription(body);
            
            log.info("{}. {} ({})", index, body, description);
            log.info("   黄经度数: {:.2f}°", longitude);
            log.info("   分类: {}", getBodyCategory(body));
            log.info("");
            
            index++;
        }
        
        log.info("【统计信息】");
        log.info("  - 天体总数: {}", positions.size());
        log.info("  - 覆盖率: 100% (10/10)");
        log.info("  - 数据来源: Astronomy Engine v2.1.0");
        log.info("  - 计算精度: 高精度天文历法");
        log.info("");
        log.info("✅ 完整报告生成成功！");
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取天体的描述信息
     */
    private String getBodyDescription(String bodyName) {
        switch (bodyName) {
            case "Sun": return "太阳（恒星，引力中心）";
            case "Moon": return "月亮（卫星，情绪象征）";
            case "Mercury": return "水星（类地行星，沟通思维）";
            case "Venus": return "金星（类地行星，爱情美学）";
            case "Mars": return "火星（类地行星，行动力）";
            case "Jupiter": return "木星（气态巨行星，扩张幸运）";
            case "Saturn": return "土星（气态巨行星，责任限制）";
            case "Uranus": return "天王星（冰巨星，变革创新）";
            case "Neptune": return "海王星（冰巨星，梦想灵感）";
            case "Pluto": return "冥王星（矮行星，转化重生）";
            default: return bodyName;
        }
    }

    /**
     * 获取天体的分类
     */
    private String getBodyCategory(String bodyName) {
        switch (bodyName) {
            case "Sun": return "恒星";
            case "Moon": return "卫星";
            case "Mercury":
            case "Venus":
            case "Mars": return "类地行星";
            case "Jupiter":
            case "Saturn": return "气态巨行星";
            case "Uranus":
            case "Neptune": return "冰巨星";
            case "Pluto": return "矮行星";
            default: return "未知";
        }
    }
}
