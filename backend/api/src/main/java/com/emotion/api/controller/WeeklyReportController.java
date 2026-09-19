package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.dto.WeeklyReportData;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.service.IEmotionReportService;
import com.emotion.api.service.IWeeklyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/weekly-report")
public class WeeklyReportController {

    @Autowired
    private IWeeklyReportService weeklyReportService;

    @Autowired
    private IEmotionReportService emotionReportService;

    @Value("${wechat.weekly-report.template-id}")
    private String templateId;

    /**
     * 记录推送授权（每次用户弹窗点"允许"后调用）
     * 注意：微信订阅消息是一次性的，每次推送前都需要用户重新授权
     */
    @PostMapping("/subscribe")
    public BaseResult<Void> subscribe(@CurrentUser UserPrincipal userPrincipal,
                                      @RequestBody Map<String, String> params) {
        String reqTemplateId = params.get("templateId");
        if (reqTemplateId == null || reqTemplateId.isEmpty()) {
            return BaseResult.error("400", "模板ID不能为空");
        }
        try {
            weeklyReportService.saveSubscription(
                userPrincipal.getUserId(),
                userPrincipal.getUserOpenId(),
                reqTemplateId
            );
            return BaseResult.success(null);
        } catch (Exception e) {
            log.error("保存授权失败", e);
            return BaseResult.error("500", "授权记录失败");
        }
    }

    /**
     * 检查本周是否已授权（未被消耗的授权才有效）
     */
    @GetMapping("/check")
    public BaseResult<Map<String, Boolean>> checkSubscription(@CurrentUser UserPrincipal userPrincipal) {
        log.info("/check: userId={}, templateId={}", userPrincipal.getUserId(), templateId);
        boolean hasSubscribed = weeklyReportService.checkSubscription(
            userPrincipal.getUserId(), templateId);
        boolean hasEverSubscribed = weeklyReportService.hasEverSubscribed(
            userPrincipal.getUserId());
        Map<String, Boolean> result = new HashMap<>();
        result.put("hasSubscribed", hasSubscribed);
        result.put("hasEverSubscribed", hasEverSubscribed);
        log.info("/check result: hasSubscribed={}, hasEverSubscribed={}", hasSubscribed, hasEverSubscribed);
        return BaseResult.success(result);
    }

    /**
     * 获取订阅消息模板ID
     */
    @GetMapping("/template-id")
    public BaseResult<Map<String, String>> getTemplateId() {
        Map<String, String> result = new HashMap<>();
        result.put("templateId", templateId);
        return BaseResult.success(result);
    }

    /**
     * 获取周报详情（完整版情绪体检报告）
     * 优先从 emotion_report 表读取 AI 预生成的报告；不存在时实时调用 AI 生成
     */
    @GetMapping("/detail")
    public BaseResult<WeeklyReportData> getWeeklyReportDetail(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            if (startDate == null || endDate == null) {
                LocalDate today = LocalDate.now();
                LocalDate lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
                LocalDate lastSunday = lastMonday.plusDays(6);
                startDate = lastMonday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                endDate = lastSunday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            WeeklyReportData report = emotionReportService.getReportDetail(
                userPrincipal.getUserId(), startDate, endDate);
            return BaseResult.success(report);
        } catch (Exception e) {
            log.error("获取周报详情失败", e);
            return BaseResult.error("500", "获取周报详情失败");
        }
    }

    /**
     * 预览周报（测试用）
     */
    @GetMapping("/preview")
    public BaseResult<WeeklyReportData> previewWeeklyReport(@CurrentUser UserPrincipal userPrincipal) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate lastMonday = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            LocalDate lastSunday = lastMonday.plusDays(6);
            WeeklyReportData report = emotionReportService.getReportDetail(
                userPrincipal.getUserId(),
                lastMonday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                lastSunday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            return BaseResult.success(report);
        } catch (Exception e) {
            log.error("生成周报失败", e);
            return BaseResult.error("500", "生成周报失败");
        }
    }
}
