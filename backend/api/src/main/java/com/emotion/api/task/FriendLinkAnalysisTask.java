package com.emotion.api.task;

import com.emotion.api.service.IFriendBindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FriendLinkAnalysisTask {

    @Autowired
    private IFriendBindService friendBindService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyAnalysis() {
        try {
            friendBindService.generateDailyFriendLinkAnalysisForAllBindings();
            log.info("心灵链接每日AI共鸣分析生成完成");
        } catch (Exception e) {
            log.error("心灵链接每日AI共鸣分析生成失败", e);
        }
    }
}
