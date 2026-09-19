package com.emotion.api.task;

import com.emotion.api.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 情感分析名额定时任务
 * 每天中午12点向Redis中写入10个可用名额
 */
@Slf4j
@Component
public class EmotionQuotaTask {

    @Autowired
    private RedisUtil redisUtil;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    /**
     * 每天12:00执行定时任务
     * 向Redis中写入当天的情感分析可用名额
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void setDailyEmotionQuota() {
        try {
            String today = dateFormat.format(new Date());
            String redisKey = today + "emotion";

            // 设置当天的可用名额为10，24小时后过期
            redisUtil.set(redisKey, 10, 12, TimeUnit.HOURS);

            log.info("成功设置{}的情感分析名额: 10个", today);
        } catch (Exception e) {
            log.error("设置情感分析名额失败", e);
        }
    }
}