package com.emotion.api.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.dto.WeeklyReportData;
import com.emotion.api.entity.EmotionReport;
import com.emotion.api.mapper.EmotionReportMapper;
import com.emotion.api.service.EmotionReportGenerateService;
import com.emotion.api.service.IEmotionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 情绪报告查询实现
 *
 * 优先级：
 * 1. 从 emotion_report 表查已有报告 → 直接返回
 * 2. 不存在 → 实时调用 AI 生成并存表 → 返回
 */
@Slf4j
@Service
public class EmotionReportServiceImpl implements IEmotionReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private EmotionReportMapper emotionReportMapper;

    @Autowired
    private EmotionReportGenerateService generateService;

    @Override
    public WeeklyReportData getReportDetail(Long userId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DATE_FMT);
        LocalDate end = LocalDate.parse(endDate, DATE_FMT);

        // 先查表
        LambdaQueryWrapper<EmotionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionReport::getUserId, userId)
               .eq(EmotionReport::getReportPeriod, "WEEK")
               .eq(EmotionReport::getPeriodStart, start)
               .eq(EmotionReport::getPeriodEnd, end)
               .eq(EmotionReport::getStatus, 1)
               .orderByDesc(EmotionReport::getCreateTime)
               .last("LIMIT 1");

        EmotionReport report = emotionReportMapper.selectOne(wrapper);

        if (report == null) {
            // 不存在，实时生成
            log.info("报告不存在，实时生成: userId={}, {} -> {}", userId, start, end);
            report = generateService.generateAndSave(userId, "WEEK", start, end);
        }

        return convertToDto(report);
    }

    @Override
    public WeeklyReportData getLatestWeeklyReport(Long userId) {
        LambdaQueryWrapper<EmotionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionReport::getUserId, userId)
               .eq(EmotionReport::getReportPeriod, "WEEK")
               .eq(EmotionReport::getStatus, 1)
               .orderByDesc(EmotionReport::getPeriodStart)
               .last("LIMIT 1");

        EmotionReport report = emotionReportMapper.selectOne(wrapper);
        if (report == null) {
            return null;
        }
        return convertToDto(report);
    }

    /**
     * EmotionReport 实体 → WeeklyReportData DTO（前端沿用现有结构）
     */
    private WeeklyReportData convertToDto(EmotionReport r) {
        WeeklyReportData dto = new WeeklyReportData();
        dto.setDateRange(r.getDateRange());
        dto.setStartDate(r.getPeriodStart() != null ? r.getPeriodStart().format(DATE_FMT) : "");
        dto.setEndDate(r.getPeriodEnd() != null ? r.getPeriodEnd().format(DATE_FMT) : "");
        dto.setRecordCount(r.getRecordCount() != null ? r.getRecordCount() : 0);
        dto.setCheckInDays(r.getCheckInDays() != null ? r.getCheckInDays() : 0);
        dto.setAverageScore(0.0);  // 废弃字段，填默认值
        dto.setEmotionState(r.getEmotionState());
        dto.setMainTag(r.getMainEmotion());
        dto.setScoreRank(0);
        dto.setCompositionSummary(r.getCompositionSummary());
        dto.setTrendDirection(r.getTrendDirection());
        dto.setTrendDescription(r.getTrendDescription());
        dto.setAiCommentary(r.getAiCommentary());
        dto.setAiInsight(r.getAiInsight());
        dto.setAiSuggestion(r.getAiSuggestion());

        // JSON 字段反序列化
        dto.setEmotionComposition(parseEmotionComposition(r.getEmotionComposition()));
        dto.setNextWeekForecast(parseJsonMap(r.getNextWeekForecast()));
        dto.setWeekComparison(parseJsonMap(r.getWeekComparison()));
        dto.setTriggers(parseJsonList(r.getTriggers()));
        dto.setHighPoint(parseJsonMap(r.getHighPoint()));
        dto.setLowPoint(parseJsonMap(r.getLowPoint()));
        dto.setBadges(parseJsonStringList(r.getBadges()));
        dto.setDailyScores(parseJsonList(r.getDailyScores()));
        dto.setTurningPoints(parseJsonList(r.getTurningPoints()));

        return dto;
    }

    /** Map<String, Object> 类型的 JSON 反序列化 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return new java.util.HashMap<>();
        try {
            return JSONUtil.toBean(json, Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    /** emotionComposition 专用：Map<String, Double> */
    @SuppressWarnings("unchecked")
    private Map<String, Double> parseEmotionComposition(String json) {
        if (json == null || json.isEmpty()) return new java.util.HashMap<>();
        try {
            Map<String, Object> raw = JSONUtil.toBean(json, Map.class);
            Map<String, Double> result = new java.util.LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (v instanceof Number) result.put(k, ((Number) v).doubleValue());
            });
            return result;
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        try {
            List raw = JSONUtil.toList(json, Map.class);
            return (List<Map<String, Object>>) raw;
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
}
