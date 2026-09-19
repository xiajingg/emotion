package com.emotion.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.repository.mapper.DailyHoroscopeMapper;
import com.emotion.api.repository.po.DailyHoroscope;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.WechatUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 每日星座运势控制器
 */
@RestController
@RequestMapping("/api/horoscope")
@RequiredArgsConstructor
public class HoroscopeController {

    private final DailyHoroscopeMapper horoscopeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WechatUserService wechatUserService;

    /**
     * 获取指定星座的今日运势（兼容旧接口）
     */
    @GetMapping("/today/{sign}")
    public BaseResult getTodayHoroscope(@PathVariable String sign) {
        return getDailyHoroscope(sign, null);
    }
    
    /**
     * 获取当前登录用户的今日专属运势（小程序专用）
     * 
     * 🔑 关键特性：
     * 1. 无需前端传参，后端自动从 Token 中识别用户
     * 2. 自动从数据库读取用户已设置的星座
     * 3. 默认查询当日运势
     * 4. 若用户未设置星座，返回明确错误提示
     * 
     * @param userPrincipal 当前登录用户（通过 @CurrentUser 注解注入）
     * @return 包含所有字段的运势数据
     */
    @GetMapping("/my-today")
    public BaseResult getMyTodayHoroscope(@CurrentUser UserPrincipal userPrincipal) {
        // 1. 从数据库获取用户信息
        WechatUser wechatUser = wechatUserService.getById(userPrincipal.getUserId());
        
        if (wechatUser == null) {
            return BaseResult.error("用户不存在");
        }
        
        String constellation = wechatUser.getConstellation();
        
        // 2. 检查用户是否已设置星座
        if (constellation == null || constellation.trim().isEmpty()) {
            return BaseResult.error("400", "您尚未设置星座，请先在'我的'页面设置星座");
        }
        
        // 3. 映射中文星座到英文标识
        String sign = mapChineseToEnglish(constellation);
        
        if (sign == null) {
            return BaseResult.error("无效的星座设置：" + constellation);
        }
        
        // 4. 调用原有的 getDailyHoroscope 逻辑
        return getDailyHoroscope(sign, null);
    }
    
    /**
     * 映射中文星座名到英文标识
     */
    private String mapChineseToEnglish(String chineseName) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("白羊座", "Aries");
        mapping.put("金牛座", "Taurus");
        mapping.put("双子座", "Gemini");
        mapping.put("巨蟹座", "Cancer");
        mapping.put("狮子座", "Leo");
        mapping.put("处女座", "Virgo");
        mapping.put("天秤座", "Libra");
        mapping.put("天蝎座", "Scorpio");
        mapping.put("射手座", "Sagittarius");
        mapping.put("摩羯座", "Capricorn");
        mapping.put("水瓶座", "Aquarius");
        mapping.put("双鱼座", "Pisces");
        return mapping.get(chineseName);
    }
    
    /**
     * 获取指定日期的星座运势（新接口，推荐小程序使用）
     * 
     * @param sign 星座英文名（Aries, Taurus, Gemini...）
     * @param date 日期（格式：yyyy-MM-dd），不传则默认为今天
     * @return 包含所有字段的运势数据
     */
    @GetMapping("/daily")
    public BaseResult getDailyHoroscope(
            @RequestParam String sign,
            @RequestParam(required = false) String date) {
        
        LocalDate targetDate = (date != null && !date.isEmpty()) 
                ? LocalDate.parse(date) 
                : LocalDate.now();
        
        String cacheKey = "horoscope:" + targetDate + ":" + sign;
        
        // 1. 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return BaseResult.success(cached);
        }
        
        // 2. 从数据库查询
        LambdaQueryWrapper<DailyHoroscope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyHoroscope::getDate, targetDate)
               .eq(DailyHoroscope::getZodiacSign, sign);
        
        DailyHoroscope horoscope = horoscopeMapper.selectOne(wrapper);
        
        if (horoscope == null) {
            return BaseResult.error("该日期的运势尚未生成，请稍后再试");
        }
        
        // 3. 构建前端友好的响应结构
        Map<String, Object> response = buildFrontendResponse(horoscope);
        
        // 4. 存入缓存（24小时）
        redisTemplate.opsForValue().set(cacheKey, response, 24, TimeUnit.HOURS);
        
        return BaseResult.success(response);
    }
    
    /**
     * 构建前端友好的响应数据结构
     */
    private Map<String, Object> buildFrontendResponse(DailyHoroscope horoscope) {
        Map<String, Object> result = new HashMap<>();
        // 🔑 关键修复：将 LocalDate 转换为字符串，避免 Redis 序列化失败
        result.put("date", horoscope.getDate().toString());
        result.put("zodiacSign", horoscope.getZodiacSign());
        result.put("zodiacName", getZodiacChineseName(horoscope.getZodiacSign()));
        result.put("astroAnalysis", horoscope.getAstroAnalysis());
        result.put("content", horoscope.getContent());
        result.put("aiAdvice", horoscope.getAiAdvice());
        result.put("dosAndDonts", horoscope.getDosAndDonts());
        result.put("loveFortune", horoscope.getLoveFortune());
        result.put("wealthFortune", horoscope.getWealthFortune());
        result.put("careerFortune", horoscope.getCareerFortune());
        return result;
    }
    
    /**
     * 获取星座中文名称
     */
    private String getZodiacChineseName(String sign) {
        Map<String, String> zodiacNames = new HashMap<>();
        zodiacNames.put("Aries", "白羊座");
        zodiacNames.put("Taurus", "金牛座");
        zodiacNames.put("Gemini", "双子座");
        zodiacNames.put("Cancer", "巨蟹座");
        zodiacNames.put("Leo", "狮子座");
        zodiacNames.put("Virgo", "处女座");
        zodiacNames.put("Libra", "天秤座");
        zodiacNames.put("Scorpio", "天蝎座");
        zodiacNames.put("Sagittarius", "射手座");
        zodiacNames.put("Capricorn", "摩羯座");
        zodiacNames.put("Aquarius", "水瓶座");
        zodiacNames.put("Pisces", "双鱼座");
        return zodiacNames.getOrDefault(sign, sign);
    }
}
