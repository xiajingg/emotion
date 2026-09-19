package com.emotion.api.service;

import io.github.cosinekitty.astronomy.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * 天文计算服务 - 基于 Astronomy Engine
 */
@Slf4j
@Service
public class AstronomyService {

    /**
     * 获取指定日期的关键天体位置数据
     * 
     * 🔑 包含标准占星学常用的 10 个主要天体：
     * - 太阳、月亮（2个）
     * - 类地行星：水星、金星、火星（3个）
     * - 气态巨行星：木星、土星（2个）
     * - 冰巨星：天王星、海王星（2个）
     * - 矮行星：冥王星（1个）
     *
     * @param date 日期（支持历史/未来日期）
     * @return 天体黄经数据 (Key: Body Name, Value: Ecliptic Longitude in degrees)
     */
    public Map<String, Double> getCelestialPositions(LocalDate date) {
        // 使用 UTC 时间中午 12:00 进行计算，以确保全球一致性
        // Astronomy Engine 的 Time 构造函数接收的是 Terrestrial Time (TT)，单位是天（相对于 J2000.0）
        // 但更方便的是使用 fromMillisecondsSince1970
        long millis = date.atTime(12, 0).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        var time = Time.fromMillisecondsSince1970(millis);
        
        Map<String, Double> positions = new HashMap<>();
        
        // 🔑 优化：扩展至完整的 10 个主要天体（标准占星学配置）
        Body[] bodies = {
            Body.Sun,      // 太阳 ☀️
            Body.Moon,     // 月亮 🌙
            Body.Mercury,  // 水星 ☿️
            Body.Venus,    // 金星 ♀️
            Body.Mars,     // 火星 ♂️
            Body.Jupiter,  // 木星 ♃
            Body.Saturn,   // 土星 ♄
            Body.Uranus,   // 天王星 ♅
            Body.Neptune,  // 海王星 ♆
            Body.Pluto     // 冥王星 ♇
        };
        
        for (Body body : bodies) {
            try {
                // Astronomy Engine 提供的方法：先获取地心向量，再转换为黄道坐标
                // 使用 Aberration.None 避免光行时计算不收敛的问题
                var geoVector = Astronomy.geoVector(body, time, Aberration.None);
                var ecliptic = Astronomy.equatorialToEcliptic(geoVector);
                
                positions.put(body.toString(), ecliptic.getElon());
            } catch (Exception e) {
                log.error("计算天体 {} 位置失败", body, e);
            }
        }
        
        log.info("成功计算日期 {} 的 {} 个天体位置", date, positions.size());
        return positions;
    }
}
