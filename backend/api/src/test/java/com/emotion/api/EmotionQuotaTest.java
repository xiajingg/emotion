package com.emotion.api;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.controller.EmotionQuotaController;
import com.emotion.api.repository.po.ActivityFreeUsage;
import com.emotion.api.task.EmotionQuotaTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class EmotionQuotaTest {

    @Autowired
    private EmotionQuotaTask emotionQuotaTask;
    @Test
    public void test() {
        emotionQuotaTask.setDailyEmotionQuota();
    }

    @Autowired
    private EmotionQuotaController emotionQuotaController;
    @Test
    public void test2() {
        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setUserOpenId("ofG8O7fqvDJfA4tCNtVGgDvWiW88");
        userPrincipal.setUserId(1L);
        emotionQuotaController.acquireQuota(userPrincipal);
    }

    @Test
    public void test3() {
        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setUserOpenId("ofG8O7fqvDJfA4tCNtVGgDvWiW88");
        userPrincipal.setUserId(1L);
        BaseResult<List<ActivityFreeUsage>> historyQuota = emotionQuotaController.getHistoryQuota(userPrincipal);
        System.out.println(historyQuota);
    }
}
