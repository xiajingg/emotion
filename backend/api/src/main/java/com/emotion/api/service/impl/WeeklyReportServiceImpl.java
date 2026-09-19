package com.emotion.api.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.emotion.api.dto.WeeklyReportData;
import com.emotion.api.entity.UserWeeklyReportActionLog;
import com.emotion.api.entity.UserWeeklyReportSubscription;
import com.emotion.api.mapper.UserWeeklyReportActionLogMapper;
import com.emotion.api.mapper.UserWeeklyReportSubscriptionMapper;
import com.emotion.api.service.IEmotionReportService;
import com.emotion.api.service.IWeeklyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 周报推送服务
 *
 * 订阅管理 + 微信推送 + 定时推送任务。
 * 报告生成已全部委托给 {@link EmotionReportGenerateService}（调用真实 AI），
 * 不再使用 if-else 模板拼凑。
 */
@Slf4j
@Service
public class WeeklyReportServiceImpl implements IWeeklyReportService {

    private static final String WECHAT_SUBSCRIBE_MESSAGE_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int ERR_USER_REFUSE_ACCEPT_MESSAGE = 43101;

    @Autowired
    private UserWeeklyReportSubscriptionMapper subscriptionMapper;

    @Autowired
    private UserWeeklyReportActionLogMapper actionLogMapper;

    @Autowired
    private WechatTokenService wechatTokenService;

    @Autowired
    private IEmotionReportService emotionReportService;

    // ==================== 订阅管理 ====================

    @Override
    @Transactional
    public void saveSubscription(Long userId, String openId, String templateId) {
        // 1. 更新状态表（用于 checkSubscription / batchSend 查询）
        LambdaQueryWrapper<UserWeeklyReportSubscription> qw = new LambdaQueryWrapper<>();
        qw.eq(UserWeeklyReportSubscription::getUserId, userId)
          .eq(UserWeeklyReportSubscription::getTemplateId, templateId);

        UserWeeklyReportSubscription existing = subscriptionMapper.selectOne(qw);
        if (existing != null) {
            // 用 LambdaUpdateWrapper 显式设置 lastSendTime=null，
            // 避开 MyBatis-Plus updateById 默认跳过 null 的问题
            LambdaUpdateWrapper<UserWeeklyReportSubscription> uw = new LambdaUpdateWrapper<>();
            uw.eq(UserWeeklyReportSubscription::getId, existing.getId())
              .set(UserWeeklyReportSubscription::getStatus, 1)
              .set(UserWeeklyReportSubscription::getSubscribeTime, LocalDateTime.now())
              .set(UserWeeklyReportSubscription::getLastSendTime, null);
            subscriptionMapper.update(null, uw);
        } else {
            UserWeeklyReportSubscription sub = new UserWeeklyReportSubscription();
            sub.setUserId(userId);
            sub.setOpenId(openId);
            sub.setTemplateId(templateId);
            sub.setSubscribeTime(LocalDateTime.now());
            sub.setStatus(1);
            subscriptionMapper.insert(sub);
        }

        // 2. 记录行为日志（用户画像：每次订阅独立一行，不覆盖）
        UserWeeklyReportActionLog actionLog = new UserWeeklyReportActionLog();
        actionLog.setUserId(userId);
        actionLog.setOpenId(openId);
        actionLog.setTemplateId(templateId);
        actionLog.setActionType("SUBSCRIBE");
        actionLog.setActionTime(LocalDateTime.now());
        actionLogMapper.insert(actionLog);

        log.info("用户 {} 授权周报推送成功，已记录行为日志", userId);
    }

    @Override
    public boolean checkSubscription(Long userId, String templateId) {
        LambdaQueryWrapper<UserWeeklyReportSubscription> qw = new LambdaQueryWrapper<>();
        qw.eq(UserWeeklyReportSubscription::getUserId, userId)
          .eq(UserWeeklyReportSubscription::getTemplateId, templateId)
          .eq(UserWeeklyReportSubscription::getStatus, 1)
          .apply("last_send_time IS NULL");  // 用 apply 代替 isNull 避免 MP 版本兼容问题
        Long count = subscriptionMapper.selectCount(qw);
        log.info("checkSubscription: userId={}, templateId={}, count={}", userId, templateId, count);
        return count > 0;
    }

    @Override
    public boolean hasEverSubscribed(Long userId) {
        LambdaQueryWrapper<UserWeeklyReportActionLog> qw = new LambdaQueryWrapper<>();
        qw.eq(UserWeeklyReportActionLog::getUserId, userId)
          .eq(UserWeeklyReportActionLog::getActionType, "SUBSCRIBE");
        return actionLogMapper.selectCount(qw) > 0;
    }

    // ==================== 报告生成（委托 AI） ====================

    @Override
    public WeeklyReportData generateWeeklyReport(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);
        return generateWeeklyReportDetail(userId,
                lastMonday.format(DATE_FMT), lastSunday.format(DATE_FMT));
    }

    @Override
    public WeeklyReportData generateWeeklyReportDetail(Long userId, String startDate, String endDate) {
        return emotionReportService.getReportDetail(userId, startDate, endDate);
    }

    // ==================== 微信推送 ====================

    @Override
    public boolean sendWeeklyReport(UserWeeklyReportSubscription subscription) {
        try {
            WeeklyReportData report = generateWeeklyReport(subscription.getUserId());
            String accessToken = wechatTokenService.getAccessToken();

            JSONObject message = new JSONObject();
            message.set("touser", subscription.getOpenId());
            message.set("template_id", subscription.getTemplateId());
            message.set("page", "/pages/weekly-report/weekly-report?startDate="
                    + report.getStartDate() + "&endDate=" + report.getEndDate());

            // 格式化日期：2026-05-22 → 2026年05月22日
            String formattedDate = report.getEndDate().substring(0, 4) + "年"
                    + report.getEndDate().substring(5, 7) + "月"
                    + report.getEndDate().substring(8, 10) + "日";

            JSONObject data = new JSONObject();
            data.set("thing1", new JSONObject().set("value", "你的下周情绪预案已生成"));
            data.set("time2", new JSONObject().set("value", formattedDate));
            data.set("thing4", new JSONObject().set("value", truncate(
                    (report.getTrendDirection() != null && !report.getTrendDirection().equals("数据不足")
                            ? "趋势" + report.getTrendDirection() + "，" : "")
                    + "AI发现你的情绪触发点", 20)));
            data.set("thing5", new JSONObject().set("value", "触发点、风险场景和应对话术"));
            data.set("thing6", new JSONObject().set("value", "点击查看下周预案 >>"));
            message.set("data", data);

            String url = WECHAT_SUBSCRIBE_MESSAGE_URL + "?access_token=" + accessToken;
            String response = HttpUtil.post(url, message.toString());
            JSONObject result = JSONUtil.parseObj(response);
            Integer errCode = result.getInt("errcode");

            if (errCode != null && errCode == 0) {
                log.info("周报推送成功，userId={}", subscription.getUserId());
                markSubscriptionConsumed(subscription);

                // 记录行为日志（用户画像：推送成功）
                insertPushLog(subscription, true, null, null);
                return true;
            } else {
                log.error("周报推送失败，errcode={}, errmsg={}, userId={}",
                        errCode, result.getStr("errmsg"), subscription.getUserId());

                // 记录行为日志（用户画像：推送失败）
                insertPushLog(subscription, false,
                        String.valueOf(errCode), result.getStr("errmsg"));

                if (isSubscriptionConsumedError(errCode)) {
                    markSubscriptionConsumed(subscription);
                    log.warn("周报订阅已因微信拒收被消费，userId={}, errcode={}",
                            subscription.getUserId(), errCode);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("发送周报异常，userId={}", subscription.getUserId(), e);

            // 记录行为日志（用户画像：推送异常）
            insertPushLog(subscription, false, "EXCEPTION", e.getMessage());
            return false;
        }
    }

    /**
     * 微信订阅消息是一次性授权。43101 表示用户拒收/授权不可用，
     * 继续保留 status=1 会导致下次任务重复捞取同一条无效授权。
     */
    private boolean isSubscriptionConsumedError(Integer errCode) {
        return errCode != null && errCode == ERR_USER_REFUSE_ACCEPT_MESSAGE;
    }

    private void markSubscriptionConsumed(UserWeeklyReportSubscription subscription) {
        subscription.setLastSendTime(LocalDateTime.now());
        subscription.setStatus(0);
        subscriptionMapper.updateById(subscription);
    }

    /**
     * 记录推送行为日志（用户画像用，每次推送独立一行）
     */
    private void insertPushLog(UserWeeklyReportSubscription subscription,
                               boolean success, String errCode, String errMsg) {
        try {
            UserWeeklyReportActionLog actionLog = new UserWeeklyReportActionLog();
            actionLog.setUserId(subscription.getUserId());
            actionLog.setOpenId(subscription.getOpenId());
            actionLog.setTemplateId(subscription.getTemplateId());
            actionLog.setActionType("PUSH");
            actionLog.setActionTime(LocalDateTime.now());
            actionLog.setPushSuccess(success ? 1 : 0);
            actionLog.setPushErrCode(errCode);
            actionLog.setPushErrMsg(errMsg);
            actionLogMapper.insert(actionLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("记录推送行为日志失败，userId={}", subscription.getUserId(), e);
        }
    }

    @Override
    @Transactional
    public void batchSendWeeklyReports() {
        log.info("开始批量推送情绪周报...");
        LambdaQueryWrapper<UserWeeklyReportSubscription> qw = new LambdaQueryWrapper<>();
        qw.eq(UserWeeklyReportSubscription::getStatus, 1)
          .isNull(UserWeeklyReportSubscription::getLastSendTime);
        List<UserWeeklyReportSubscription> subs = subscriptionMapper.selectList(qw);
        log.info("找到 {} 个待推送订阅", subs.size());

        int success = 0, fail = 0;
        for (UserWeeklyReportSubscription sub : subs) {
            try {
                if (sendWeeklyReport(sub)) {
                    success++;
                } else {
                    fail++;
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("推送失败，userId={}", sub.getUserId(), e);
                fail++;
            }
        }
        log.info("周报推送完成，成功: {}, 失败: {}", success, fail);
    }

    private String truncate(String str, int max) {
        if (str == null) return "";
        return str.length() <= max ? str : str.substring(0, max);
    }
}
