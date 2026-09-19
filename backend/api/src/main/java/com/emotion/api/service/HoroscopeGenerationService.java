package com.emotion.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.emotion.api.repository.mapper.DailyHoroscopeMapper;
import com.emotion.api.repository.po.DailyHoroscope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 每日星座运势生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoroscopeGenerationService {

    private final AstronomyService astronomyService;
    private final HoroscopeRuleEngine ruleEngine;
    private final OllamaDirectService ollamaDirectService;  // 改用 OllamaDirectService
    private final DailyHoroscopeMapper horoscopeMapper;

    private static final String[] ZODIAC_SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", 
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };

    /**
     * 生成指定日期的所有星座运势
     * 注意：移除了 @Transactional，每个星座独立插入，避免长事务导致的连接超时
     */
    public void generateDailyHoroscopes(LocalDate date) {
        log.info("开始生成日期 {} 的每日星座运势", date);
        
        // 1. 获取天文数据（所有星座共享）
        Map<String, Double> positions = astronomyService.getCelestialPositions(date);
        
        // 2. 为每个星座独立生成运势
        for (String sign : ZODIAC_SIGNS) {
            try {
                // 幂等性检查：查询是否已存在该星座的记录
                LambdaQueryWrapper<DailyHoroscope> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DailyHoroscope::getDate, date)
                       .eq(DailyHoroscope::getZodiacSign, sign);
                
                long existingCount = horoscopeMapper.selectCount(wrapper);
                if (existingCount > 0) {
                    log.info("星座 {} 在日期 {} 的运势已存在（{} 条记录），跳过", sign, date, existingCount);
                    continue;
                }
                
                log.info("开始生成星座 {} 的运势...", sign);
                
                // 为当前星座生成个性化的占星数据
                Map<String, String> horoscopeData = ruleEngine.generateHoroscopeData(positions, sign);
                
                // 调用 AI 生成个性化建议（使用 OllamaDirectService）
                String prompt = String.format(
                    "你是一位面向年轻用户的占星情绪陪伴顾问，语言要短、具体、可执行。\n" +
                    "基于以下星象摘要和规则解读，为该星座（%s）生成每日建议。不要堆天文术语，不要复述黄经度数。\n\n" +
                    "【星象摘要】\n%s\n\n" +
                    "【规则解读】\n%s\n\n" +
                    "【严格要求】必须以纯JSON格式返回，包含以下所有字段（不可省略）：\n" +
                    "- ai_advice: 字符串，温暖贴心的建议（50-80字），只讲今天最值得注意的一件事和一个行动\n" +
                    "- dos_and_donts: 字符串，今日宜忌事项（例如：'宜：沟通、整理；忌：硬扛、冲动回复'）\n" +
                    "- love_fortune: 整数，情感运势分数（0-100），根据月金相位和金星位置评估\n" +
                    "- wealth_fortune: 整数，财富运势分数（0-100），根据木星和土星位置评估\n" +
                    "- career_fortune: 整数，事业运势分数（0-100），根据火星和太阳位置评估\n\n" +
                    "【示例输出】\n" +
                    "{\n" +
                    "  \"ai_advice\": \"今天月亮与金星呈和谐相位，适合与朋友聚会或表达心意。同时火星能量强劲，适合推进工作计划。\",\n" +
                    "  \"dos_and_donts\": \"宜：沟通、约会、主动出击；忌：熬夜、争吵、冲动投资\",\n" +
                    "  \"love_fortune\": 75,\n" +
                    "  \"wealth_fortune\": 60,\n" +
                    "  \"career_fortune\": 70\n" +
                    "}\n\n" +
                    "请直接返回JSON，不要添加任何解释文字。",
                    getZodiacChineseName(sign),
                    horoscopeData.get("astro_analysis"),
                    horoscopeData.get("content")
                );
                
                // 使用 OllamaDirectService 直接调用 API（参考 submitText 接口实现）
                String rawJson = callOllamaForHoroscope(prompt);
                
                // 调试日志：打印 AI 原始返回
                log.info("星座 {} - AI 原始返回: {}", sign, rawJson);
                
                String extractedJson = extractJsonFromResponse(rawJson);
                log.info("星座 {} - 提取后的 JSON: {}", sign, extractedJson);
                
                JSONObject json;
                try {
                    json = JSONUtil.parseObj(extractedJson);
                } catch (Exception e) {
                    log.error("星座 {} - JSON 解析失败，使用空对象", sign, e);
                    json = new JSONObject();
                }
                
                // 构建运势对象
                DailyHoroscope horoscope = new DailyHoroscope();
                horoscope.setDate(date);
                horoscope.setZodiacSign(sign);
                
                // 【层次1】原始天文数据
                horoscope.setAstroAnalysis(horoscopeData.get("astro_analysis"));
                // 【层次2】规则引擎解析
                horoscope.setContent(horoscopeData.get("content"));
                // 【层次3】AI 个性化建议
                horoscope.setAiAdvice(json.getStr("ai_advice"));
                horoscope.setDosAndDonts(json.getStr("dos_and_donts"));
                
                // 【数值字段】提供默认值确保不为 null
                horoscope.setLoveFortune(json.getInt("love_fortune", 50));
                horoscope.setWealthFortune(json.getInt("wealth_fortune", 50));
                horoscope.setCareerFortune(json.getInt("career_fortune", 50));
                
                horoscope.setAiModel("ollama"); 
                
                // 单条插入，自动提交
                int insertResult = horoscopeMapper.insert(horoscope);
                if (insertResult > 0) {
                    log.info("✅ 成功生成并保存星座 {} 的运势 (ID: {})", sign, horoscope.getId());
                } else {
                    log.warn("⚠️ 星座 {} 的运势插入失败", sign);
                }
                
            } catch (Exception e) {
                // 异常隔离：单个星座失败不影响其他星座
                log.error("❌ 生成星座 {} 的运势失败，错误信息: {}", sign, e.getMessage(), e);
            }
        }
        
        log.info("日期 {} 的每日星座运势生成完成", date);
    }

    /**
     * 调用 Ollama API 生成星座运势建议（参考 submitText 接口实现）
     */
    private String callOllamaForHoroscope(String prompt) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", "qwen3.5:9b");
            requestBody.put("stream", false);
            requestBody.put("think", false);  // 关闭思考模式
            
            Map<String, String> message = new java.util.HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Object[]{message});
            
            // 发送 HTTP 请求
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                cn.hutool.json.JSONUtil.toJsonStr(requestBody),
                okhttp3.MediaType.parse("application/json")
            );
            
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://localhost:11434/api/chat")
                .post(body)
                .build();
            
            try (okhttp3.Response response = createHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("HTTP请求失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.debug("Ollama原始响应: {}", responseBody);
                
                // 解析响应
                cn.hutool.json.JSONObject responseJson = cn.hutool.json.JSONUtil.parseObj(responseBody);
                cn.hutool.json.JSONObject messageObj = responseJson.getJSONObject("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }
                
                return messageObj.getStr("content");
            }
        } catch (Exception e) {
            log.error("调用 Ollama API 失败", e);
            return "{}";
        }
    }
    
    /**
     * 创建 HTTP 客户端（复用配置）
     */
    private okhttp3.OkHttpClient createHttpClient() {
        return new okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
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

    /**
     * 从AI响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        String trimmed = response.trim();
        
        // 提取Markdown代码块 ```json ... ```
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String jsonContent = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                if (jsonContent.toLowerCase().startsWith("json")) {
                    jsonContent = jsonContent.substring(4).trim();
                }
                return jsonContent;
            }
        }
        
        // 直接查找JSON对象 { ... }
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1);
        }
        
        return trimmed;
    }
}
