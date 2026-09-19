package com.emotion.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.dao.rds.DailyMotivationMapper;
import com.emotion.api.repository.po.DailyMotivation;
import com.emotion.api.task.DailyMotivationTask;
import com.emotion.api.util.ZhipuAiUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class MotivationTest {
        @Autowired
    private DailyMotivationMapper dailyMotivationMapper;

    @Test // 每天00:00执行
    public void saveDailyMotivation() {
        String last100DaysMotivationContent = getLast100DaysMotivationContent();
        String userMessage = "给我说一句励志且幽默的句子, 不要跟上面的几句内容和意义重复, 尽量不要出现重复的词语, 不要讲自信,谦虚和成功的句子. 输出参考示例: 别低估别人高估自己，以免陷入盲目自信的毒鸡汤之中无法自拔。";
        
        String result = ZhipuAiUtil.chat(last100DaysMotivationContent + userMessage);

        DailyMotivation dailyMotivation = new DailyMotivation();
        dailyMotivation.setContent(result);
        // 设置第二天的日期
        dailyMotivation.setDate(LocalDate.now().plusDays(1));
        LocalDateTime nowTime = LocalDateTime.now();
        dailyMotivation.setCreatedTime(nowTime);
        dailyMotivation.setUpdatedTime(nowTime);

    }


    /**
     * 查询前 100 天的毒鸡汤内容
     *
     * @return 前 100 天的毒鸡汤内容列表
     */
    private String getLast100DaysMotivationContent() {
        // 获取当前日期和前 100 天的日期
        LocalDate today = LocalDate.now();
        LocalDate hundredDaysAgo = today.minusDays(100);

        // 使用 LambdaQueryWrapper 查询
        LambdaQueryWrapper<DailyMotivation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(DailyMotivation::getDate, hundredDaysAgo, today)
                .orderByDesc(DailyMotivation::getDate); // 按日期降序排序

        // 查询数据库，返回结果
        List<DailyMotivation> motivations = dailyMotivationMapper.selectList(queryWrapper);

        // 提取 content 字段
        String collect = motivations.stream()
                .map(c -> c.getContent() + "\n----------------------")
                .collect(Collectors.joining("\n"));

        return collect;
    }


    @Autowired
    private DailyMotivationTask dailyMotivationTask;

    @Test
    public void testSaveDailyMotivation() {
        dailyMotivationTask.saveDailyMotivation();
    }
}
