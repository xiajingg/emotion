package com.emotion.api.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.entity.EmotionReport;
import com.emotion.api.mapper.EmotionReportMapper;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.po.UserTextInteraction;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 情绪报告生成服务
 * 
 * 核心逻辑：查询用户一段时间内的交互记录 → 构建 Prompt → 调用 Ollama AI 分析 → 存储报告
 * 不再用 Java if-else 拼模板冒充 AI，而是真正让大模型深度分析
 */
@Slf4j
@Service
public class EmotionReportGenerateService {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    private static final String MODEL = "qwen3.5:9b";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MD_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)  // 周报分析数据量大，给足时间
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Autowired
    private UserTextInteractionMapper interactionMapper;

    @Autowired
    private EmotionReportMapper emotionReportMapper;

    // ================================================================
    //  公开方法：生成并保存报告
    // ================================================================

    /**
     * 为指定用户生成指定周期的报告（定时任务/手动调用）
     *
     * @param userId       用户ID
     * @param reportPeriod 周期类型：WEEK / MONTH
     * @param periodStart  周期开始日期
     * @param periodEnd    周期结束日期
     * @return 生成的报告实体
     */
    public EmotionReport generateAndSave(Long userId, String reportPeriod,
                                          LocalDate periodStart, LocalDate periodEnd) {
        log.info("开始生成报告: userId={}, period={}, {} -> {}", userId, reportPeriod, periodStart, periodEnd);

        // 1. 查本周记录
        List<UserTextInteraction> weekRecords = queryRecords(userId, periodStart, periodEnd);

        // 2. 查上周记录（用于环比）
        LocalDate lastStart = periodStart.minusWeeks(1);
        LocalDate lastEnd = periodEnd.minusWeeks(1);
        List<UserTextInteraction> lastWeekRecords = queryRecords(userId, lastStart, lastEnd);

        // 3. 统计基础数据
        int recordCount = weekRecords.size();
        int checkInDays = (int) weekRecords.stream()
                .map(r -> r.getCreateTime().toLocalDate()).distinct().count();

        if (recordCount == 0) {
            // 无记录，生成空报告
            EmotionReport empty = buildEmptyReport(userId, reportPeriod, periodStart, periodEnd);
            emotionReportMapper.insert(empty);
            log.info("用户 {} 无记录，已生成空报告", userId);
            return empty;
        }

        // 4. 构建 Prompt 并调用 AI
        String promptPayload = buildReportPrompt(weekRecords, lastWeekRecords,
                periodStart, periodEnd, checkInDays);
        String aiResponseJson;
        try {
            aiResponseJson = callOllamaForReport(promptPayload);
        } catch (Exception e) {
            log.error("AI 报告生成失败，降级为空报告: userId={}", userId, e);
            EmotionReport fallback = buildEmptyReport(userId, reportPeriod, periodStart, periodEnd);
            fallback.setRecordCount(recordCount);
            fallback.setCheckInDays(checkInDays);
            fallback.setAiCommentary("AI 分析服务暂时繁忙，请稍后再试～");
            emotionReportMapper.insert(fallback);
            return fallback;
        }

        // 5. 解析 AI 返回的 JSON → 填充报告实体
        EmotionReport report = parseAiResponse(aiResponseJson, userId, reportPeriod,
                periodStart, periodEnd, recordCount, checkInDays);

        // 6. 持久化
        emotionReportMapper.insert(report);
        log.info("报告生成成功: userId={}, period={}, mainEmotion={}",
                userId, reportPeriod, report.getMainEmotion());
        return report;
    }

    /**
     * 批量生成上周所有活跃用户的周报（定时任务调用）
     */
    public int batchGenerateWeeklyReports() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("批量生成周报: {} -> {}", lastMonday, lastSunday);

        // 找出上周有记录的所有用户
        List<Long> activeUserIds = findActiveUsers(lastMonday, lastSunday);
        log.info("上周活跃用户数: {}", activeUserIds.size());

        int success = 0, fail = 0;
        for (Long userId : activeUserIds) {
            try {
                // 先检查是否已经生成过
                LambdaQueryWrapper<EmotionReport> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(EmotionReport::getUserId, userId)
                        .eq(EmotionReport::getReportPeriod, "WEEK")
                        .eq(EmotionReport::getPeriodStart, lastMonday)
                        .eq(EmotionReport::getStatus, 1);
                if (emotionReportMapper.selectCount(checkWrapper) > 0) {
                    log.debug("用户 {} 周报已存在，跳过", userId);
                    continue;
                }

                generateAndSave(userId, "WEEK", lastMonday, lastSunday);
                success++;
                // 避免 Ollama 过载，适当间隔
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("用户 {} 周报生成失败", userId, e);
                fail++;
            }
        }

        log.info("周报批量生成完成: 成功={}, 失败={}", success, fail);
        return success;
    }

    // ================================================================
    //  数据查询
    // ================================================================

    private List<UserTextInteraction> queryRecords(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<UserTextInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTextInteraction::getUserId, userId)
               .between(UserTextInteraction::getCreateTime,
                        start.atStartOfDay(), end.atTime(23, 59, 59))
               .orderByAsc(UserTextInteraction::getCreateTime);
        return interactionMapper.selectList(wrapper);
    }

    private List<Long> findActiveUsers(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<UserTextInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(UserTextInteraction::getUserId)
               .between(UserTextInteraction::getCreateTime,
                        start.atStartOfDay(), end.atTime(23, 59, 59))
               .groupBy(UserTextInteraction::getUserId);
        return interactionMapper.selectList(wrapper).stream()
                .map(UserTextInteraction::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    // ================================================================
    //  Prompt 构建：把本周记录 + 上周对照数据喂给 AI
    // ================================================================

    private String buildReportPrompt(List<UserTextInteraction> weekRecords,
                                      List<UserTextInteraction> lastWeekRecords,
                                      LocalDate periodStart, LocalDate periodEnd,
                                      int checkInDays) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Role\n");
        sb.append("你是一位擅长情绪急救、压力复盘和关系沟通的产品型教练。\n");
        sb.append("你的任务是根据用户一周的情绪记录数据，生成一份可执行的\"下周情绪预案\"：识别高频触发点、预测下周风险场景，并给出具体应对动作和可复用话术。\n\n");

        sb.append("# 规则\n");
        sb.append("1. 必须严格输出纯 JSON，不要用 ```json 包裹，不要有任何前言后语。\n");
        sb.append("2. 所有分析必须基于我提供的数据，不要凭空编造。\n");
        sb.append("3. 语气温柔、有共情力，像一位懂你的老朋友，绝对禁止爹味说教。\n");
        sb.append("4. 如果数据不足（记录少），也要如实分析并给出温暖的鼓励。\n\n");

        sb.append("# 输出 JSON Schema（所有字段必须存在）\n");
        sb.append("{\n");
        sb.append("  \"mainEmotion\": \"主打情绪标签，4-8字\",\n");
        sb.append("  \"emotionState\": \"情绪状态描述，如：愉悦·充满活力 / 平静·内心安定 / 平淡·需要关怀 / 低落·需要休息\",\n");
        sb.append("  \"emotionComposition\": {\"标签1\": 0.4, \"标签2\": 0.3},  占比小数，和为1\n");
        sb.append("  \"compositionSummary\": \"情绪成分总结文字，50-80字\",\n");
        sb.append("  \"trendDirection\": \"上升 / 下降 / 波动 / 平稳 / 数据不足\",\n");
        sb.append("  \"trendDescription\": \"趋势分析文字，60-100字\",\n");
        sb.append("  \"aiCommentary\": \"AI专属治愈点评，80-150字，像老朋友的口吻\",\n");
        sb.append("  \"aiInsight\": \"本周高频触发点：发现的关系/工作/睡前内耗模式，100-150字\",\n");
        sb.append("  \"aiSuggestion\": \"下周具体预案，分条列出，每条20-40字，用\\n换行，3-4条，必须包含'遇到X，先做Y，再说Z'式建议\",\n");
        sb.append("  \"nextWeekForecast\": {\"riskLevel\": \"high/medium/low\", \"riskDays\": \"预测风险日如周一或空字符串\", \"suggestion\": \"下周风险场景和应对话术\", \"peakDay\": \"预测峰值日或'不确定'\"},\n");
        sb.append("  \"turningPoints\": [{\"date\": \"MM-dd\", \"dayOfWeek\": \"周一\", \"description\": \"拐点描述\"}],\n");
        sb.append("  \"highPoint\": {\"date\": \"MM-dd\", \"dayOfWeek\": \"周X\", \"emotion\": \"情绪标签\", \"inputText\": \"用户当天说的话(截取前30字)\"},\n");
        sb.append("  \"lowPoint\": {\"date\": \"MM-dd\", \"dayOfWeek\": \"周X\", \"emotion\": \"情绪标签\", \"inputText\": \"用户当天说的话(截取前30字)\"},\n");
        sb.append("  \"badges\": [\"成就徽章1\", \"成就徽章2\"],\n");
        sb.append("  \"triggers\": [{\"category\": \"工作/人际/生活/健康/自我成长\", \"impact\": \"positive/negative/neutral\", \"description\": \"触发点分析文字\"}],\n");
        sb.append("  \"weekComparisonText\": \"周环比文字，如：相比上周提升了3分！你在变得更好 🌱\"\n");
        sb.append("}\n\n");

        // === 本周数据 ===
        sb.append("# 本周情绪记录（").append(periodStart.format(MD_FMT))
                .append(" → ").append(periodEnd.format(MD_FMT)).append("）\n");
        sb.append("共 ").append(weekRecords.size()).append(" 条记录，打卡 ").append(checkInDays).append(" 天\n\n");

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        for (UserTextInteraction r : weekRecords) {
            String dayOfWeek = getDayOfWeekCn(r.getCreateTime().getDayOfWeek());
            String emotion = extractEmotion(r);
            String text = truncate(r.getInputText(), 50);
            sb.append("- ").append(dayOfWeek).append(" ")
                    .append(r.getCreateTime().format(timeFmt))
                    .append(" | 情绪: ").append(emotion)
                    .append(" | \"").append(text).append("\"\n");
        }
        sb.append("\n");

        // === 上周对照数据 ===
        if (!lastWeekRecords.isEmpty()) {
            sb.append("# 上周对照数据（用于环比分析）\n");
            sb.append("共 ").append(lastWeekRecords.size()).append(" 条记录\n");
            for (UserTextInteraction r : lastWeekRecords) {
                String emotion = extractEmotion(r);
                sb.append("- ").append(getDayOfWeekCn(r.getCreateTime().getDayOfWeek()))
                        .append(" | 情绪: ").append(emotion)
                        .append(" | \"").append(truncate(r.getInputText(), 40)).append("\"\n");
            }
            sb.append("\n");
        }

        sb.append("# 请开始分析，输出上述 JSON：");

        return sb.toString();
    }

    // ================================================================
    //  Ollama API 调用
    // ================================================================

    private String callOllamaForReport(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("stream", false);
        requestBody.put("think", false);
        requestBody.put("options", Map.of("temperature", 0.7, "top_p", 0.8));

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        log.info("发送周报生成请求到 Ollama, Prompt 长度: {} 字符", prompt.length());
        long start = System.currentTimeMillis();

        RequestBody body = RequestBody.create(
                JSONUtil.toJsonStr(requestBody),
                MediaType.parse("application/json"));
        Request request = new Request.Builder().url(OLLAMA_API_URL).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama 请求失败: " + response.code());
            }
            String respBody = response.body().string();
            log.info("Ollama 周报响应耗时: {}ms", System.currentTimeMillis() - start);

            Map<String, Object> respMap = JSONUtil.toBean(respBody, Map.class);
            Map<String, Object> msgObj = (Map<String, Object>) respMap.get("message");
            if (msgObj == null || !msgObj.containsKey("content")) {
                throw new RuntimeException("Ollama 返回格式异常");
            }
            return (String) msgObj.get("content");
        }
    }

    // ================================================================
    //  AI 响应解析
    // ================================================================

    private EmotionReport parseAiResponse(String aiResponse, Long userId,
                                           String reportPeriod, LocalDate periodStart,
                                           LocalDate periodEnd, int recordCount, int checkInDays) {
        String jsonStr = extractJson(aiResponse);
        Map<String, Object> map;
        try {
            map = JSONUtil.toBean(jsonStr, Map.class);
        } catch (Exception e) {
            log.warn("AI 返回 JSON 解析失败，使用空报告。原始: {}", jsonStr);
            return buildEmptyReport(userId, reportPeriod, periodStart, periodEnd);
        }

        EmotionReport report = new EmotionReport();
        report.setUserId(userId);
        report.setReportPeriod(reportPeriod);
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setDateRange(periodStart.format(DATE_FMT) + "至" + periodEnd.format(DATE_FMT));
        report.setRecordCount(recordCount);
        report.setCheckInDays(checkInDays);
        report.setStatus(1);

        report.setMainEmotion(safeStr(map, "mainEmotion"));
        report.setEmotionState(safeStr(map, "emotionState"));
        report.setCompositionSummary(safeStr(map, "compositionSummary"));
        report.setTrendDirection(safeStr(map, "trendDirection"));
        report.setTrendDescription(safeStr(map, "trendDescription"));
        report.setAiCommentary(safeStr(map, "aiCommentary"));
        report.setAiInsight(safeStr(map, "aiInsight"));
        report.setAiSuggestion(safeStr(map, "aiSuggestion"));

        // JSON 字段
        report.setEmotionComposition(toJsonStr(map.get("emotionComposition")));
        report.setNextWeekForecast(toJsonStr(map.get("nextWeekForecast")));
        report.setWeekComparison(toJsonStr(map.get("weekComparison")));
        report.setTriggers(toJsonStr(map.get("triggers")));
        report.setHighPoint(toJsonStr(map.get("highPoint")));
        report.setLowPoint(toJsonStr(map.get("lowPoint")));
        report.setBadges(toJsonStr(map.get("badges")));
        report.setTurningPoints(toJsonStr(map.get("turningPoints")));

        // 周环比文字单独存进 weekComparison
        String cmpText = safeStr(map, "weekComparisonText");
        if (!cmpText.isEmpty() && report.getWeekComparison() == null) {
            report.setWeekComparison(JSONUtil.toJsonStr(Map.of("trendText", cmpText)));
        }

        return report;
    }

    private EmotionReport buildEmptyReport(Long userId, String reportPeriod,
                                            LocalDate start, LocalDate end) {
        EmotionReport report = new EmotionReport();
        report.setUserId(userId);
        report.setReportPeriod(reportPeriod);
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setDateRange(start.format(DATE_FMT) + "至" + end.format(DATE_FMT));
        report.setRecordCount(0);
        report.setCheckInDays(0);
        report.setMainEmotion("暂无");
        report.setEmotionState("暂无数据");
        report.setCompositionSummary("暂无数据，快去记录第一条心情吧～");
        report.setTrendDirection("暂无");
        report.setTrendDescription("暂无数据，需至少3天记录才能分析趋势");
        report.setAiCommentary("还没有数据哦，从今天开始记录你的心情吧～");
        report.setAiSuggestion("从今天开始记录卡住的场景，下周就能看到专属触发点和应对预案");
        report.setStatus(1);
        return report;
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private String extractEmotion(UserTextInteraction r) {
        try {
            if (r.getAiResponse() != null) {
                Map<String, Object> aiResp = JSONUtil.toBean(r.getAiResponse(), Map.class);
                Map<String, Object> data = (Map<String, Object>) aiResp.get("data");
                if (data != null && data.get("emotion") != null) {
                    return data.get("emotion").toString();
                }
            }
        } catch (Exception ignored) {}
        return "未分类";
    }

    private String getDayOfWeekCn(DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }

    private String truncate(String str, int max) {
        if (str == null) return "";
        return str.length() <= max ? str : str.substring(0, max) + "...";
    }

    private String safeStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private String toJsonStr(Object obj) {
        if (obj == null) return null;
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 AI 响应中提取 JSON
     */
    private String extractJson(String response) {
        if (response == null || response.trim().isEmpty()) return "{}";
        String trimmed = response.trim();

        // ```json ... ```
        int s = trimmed.indexOf("```");
        if (s != -1) {
            int e = trimmed.lastIndexOf("```");
            if (e > s) {
                String inner = trimmed.substring(s + 3, e).trim();
                if (inner.toLowerCase().startsWith("json")) inner = inner.substring(4).trim();
                return inner;
            }
        }
        // { ... }
        int braceS = trimmed.indexOf('{');
        int braceE = trimmed.lastIndexOf('}');
        if (braceS != -1 && braceE > braceS) {
            return trimmed.substring(braceS, braceE + 1);
        }
        return trimmed;
    }
}
