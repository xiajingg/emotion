package com.emotion.api.task;

import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.service.IUserFunctionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailyResetTask {

    @Autowired
    private IUserFunctionRecordService userFunctionRecordService;

    @Scheduled(cron = "0 0 0 * * ?") // 每天00:00执行
    public void resetAvailableToday() {
        List<UserFunctionRecord> records = userFunctionRecordService.list();
        // 把查出来的所有的数据的usedTimesToday字段设批量置为0并存库
        records.forEach(record -> {
            record.setUsedTimesToday(0L);
            record.setTxt2ImgDailyUsed(0L);
        });
        userFunctionRecordService.updateBatchById(records);
    }
}
