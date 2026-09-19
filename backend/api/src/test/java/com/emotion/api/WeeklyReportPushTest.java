package com.emotion.api;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.emotion.api.entity.UserWeeklyReportSubscription;
import com.emotion.api.mapper.UserWeeklyReportSubscriptionMapper;
import com.emotion.api.repository.dao.rds.WechatUserMapper;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.IWeeklyReportService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * 周报推送测试 —— 给 userId=1 发一条微信订阅消息
 * <p>
 * 注意：需要用户在小程序里先点过「订阅推送」按钮授权，否则微信会返回 errcode=43101。
 * 如果用户还未授权，本测试会模拟消耗订阅（status=0），方便验证前端按钮重新出现。
 */
@Slf4j
@SpringBootTest(classes = EmotionApplication.class)
public class WeeklyReportPushTest {

    @Autowired
    private IWeeklyReportService weeklyReportService;

    @Autowired
    private UserWeeklyReportSubscriptionMapper subscriptionMapper;

    @Autowired
    private WechatUserMapper wechatUserMapper;

    @Value("${wechat.weekly-report.template-id}")
    private String templateId;

    /**
     * 给 userId=1 推送情绪周报
     */
    @Test
    public void testPushToUser1() {
        log.info("========== 开始：为用户1推送情绪周报 ==========");

        // 1. 查出真实 openId
        WechatUser wechatUser = wechatUserMapper.selectById(1L);
        if (wechatUser == null) {
            log.error("❌ 找不到 userId=1 的微信用户，无法推送");
            return;
        }
        String realOpenId = wechatUser.getOpenId();
        log.info("📱 用户1 openId: {}", realOpenId);
        log.info("📋 模板ID: {}", templateId);

        // 2. 重置订阅状态为待推送
        LambdaUpdateWrapper<UserWeeklyReportSubscription> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserWeeklyReportSubscription::getUserId, 1L)
          .set(UserWeeklyReportSubscription::getOpenId, realOpenId)
          .set(UserWeeklyReportSubscription::getTemplateId, templateId)
          .set(UserWeeklyReportSubscription::getStatus, 1)
          .set(UserWeeklyReportSubscription::getLastSendTime, null)
          .set(UserWeeklyReportSubscription::getSubscribeTime, LocalDateTime.now());

        // 没有记录就创建
        long exists = subscriptionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserWeeklyReportSubscription>()
                        .eq(UserWeeklyReportSubscription::getUserId, 1L));
        if (exists == 0) {
            UserWeeklyReportSubscription sub = new UserWeeklyReportSubscription();
            sub.setUserId(1L);
            sub.setOpenId(realOpenId);
            sub.setTemplateId(templateId);
            sub.setStatus(1);
            sub.setSubscribeTime(LocalDateTime.now());
            subscriptionMapper.insert(sub);
            log.info("✅ 创建订阅记录: id={}", sub.getId());
        } else {
            subscriptionMapper.update(null, uw);
            log.info("✅ 强制重置订阅为待推送（lastSendTime=NULL）");
        }

        // 3. 重新查询确认
        UserWeeklyReportSubscription sub = subscriptionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserWeeklyReportSubscription>()
                        .eq(UserWeeklyReportSubscription::getUserId, 1L));
        log.info("📊 重置后确认: status={}, lastSendTime={}, openId={}",
                sub.getStatus(), sub.getLastSendTime(), sub.getOpenId());

        // 4. 推送前检查
        boolean beforePush = weeklyReportService.checkSubscription(1L, templateId);
        log.info("📋 推送前 checkSubscription = {}", beforePush);

        // 5. 真实推送
        log.info("🚀 调用 sendWeeklyReport...");
        try {
            weeklyReportService.sendWeeklyReport(sub);
        } catch (Exception e) {
            log.error("❌ sendWeeklyReport 异常: {}", e.getMessage());
        }

        // 6. 查数据库确认推送后状态
        sub = subscriptionMapper.selectById(sub.getId());
        log.info("📊 推送后: status={}, lastSendTime={}", sub.getStatus(), sub.getLastSendTime());

        if (sub.getStatus() == 0) {
            log.info("✅✅✅ 推送成功！微信消息已发送，请检查微信");
        } else {
            log.warn("⚠️ 微信推送未成功（可能原因：用户未在小程序订阅授权 errcode=43101）");
            log.info("🔧 模拟推送消耗（status→0），以便验证前端按钮重新出现...");

            // 手动模拟推送消耗：status=0, lastSendTime=当前时间
            LambdaUpdateWrapper<UserWeeklyReportSubscription> consume = new LambdaUpdateWrapper<>();
            consume.eq(UserWeeklyReportSubscription::getUserId, 1L)
                   .set(UserWeeklyReportSubscription::getStatus, 0)
                   .set(UserWeeklyReportSubscription::getLastSendTime, LocalDateTime.now());
            subscriptionMapper.update(null, consume);

            sub = subscriptionMapper.selectById(sub.getId());
            log.info("📊 模拟消耗后: status={}, lastSendTime={}", sub.getStatus(), sub.getLastSendTime());
        }

        // 7. 推送后再次检查
        boolean afterPush = weeklyReportService.checkSubscription(1L, templateId);
        log.info("📋 推送后 checkSubscription = {} (false = 前端应显示订阅按钮)", afterPush);

        if (!afterPush) {
            log.info("🎉 订阅已消耗！去小程序检查情绪周报入口是否重新出现「订阅推送」按钮");
        }

        log.info("========== 测试完成 ==========");
    }
}
