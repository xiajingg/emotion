package com.emotion.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.dto.DrinkRecordDTO;
import com.emotion.api.dto.DrinkRecordDetail;
import com.emotion.api.dto.DrinkSubmitDTO;
import com.emotion.api.repository.dao.rds.DrinkRecordMapper;
import com.emotion.api.repository.po.DrinkRecord;
import com.emotion.api.service.IDrinkService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DrinkImpl extends ServiceImpl<DrinkRecordMapper, DrinkRecord> implements IDrinkService {
    @Override
    public Integer saveSubmit(Long userId, DrinkSubmitDTO drinkIntake) {
        DrinkRecord drinkRecord = new DrinkRecord();
        drinkRecord.setUserId(userId);
        drinkRecord.setCreateTime(new Date());
        drinkRecord.setWaterIntake(drinkIntake.getDrinkIntake());
        Long uploadFileId = drinkIntake.getUploadFileId();
        // 插入图片
        if (uploadFileId != null) {
            drinkRecord.setImageId(uploadFileId);
        }
        baseMapper.insert(drinkRecord);
        return 200;
    }

    @Override
    public List<DrinkRecordDTO> getDrinkRecordByUserId(Long userId) {
        // 根据userId查询饮水记录，按时间降序排列
        LambdaQueryWrapper<DrinkRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DrinkRecord::getUserId, userId);
        queryWrapper.orderByDesc(DrinkRecord::getCreateTime);
        List<DrinkRecord> drinkRecords = baseMapper.selectList(queryWrapper);
        // format年月日和时分秒
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        List<DrinkRecordDTO> drinkRecordDTOS = new ArrayList<>();
        List<DrinkRecordDetail> drinkRecordDetails = null;
        DrinkRecordDTO drinkRecordDTO = null;
        String currentDate = "";
        if (CollUtil.isEmpty(drinkRecords)) {
            return drinkRecordDTOS;
        }
        for (DrinkRecord drinkRecord : drinkRecords) {
            // 获取年月日
            String date = dateFormat.format(drinkRecord.getCreateTime());
            // 获取时分秒
            String time = timeFormat.format(drinkRecord.getCreateTime());
            // 如果当前日期和前一条记录的日期不同，则创建一个新的DrinkRecordDTO对象
            if (!currentDate.equals(date)) {
                if (drinkRecordDTO != null) {
                    // 如果是新的一天，则将之前的记录添加到列表中
                    drinkRecordDTOS.add(drinkRecordDTO);
                }
                // 初始化drinkRecordDTO
                drinkRecordDTO = new DrinkRecordDTO();
                drinkRecordDetails = new ArrayList<>();
                drinkRecordDTO.setDate(date);
                drinkRecordDTO.setDetails(drinkRecordDetails);
                drinkRecordDTO.setTotalDrinkIntake(0);
                currentDate = date;
            }
            DrinkRecordDetail drinkRecordDetail = getDrinkRecordDetail(drinkRecord, time);
            drinkRecordDetails.add(drinkRecordDetail);
            drinkRecordDTO.setDetails(drinkRecordDetails);
            drinkRecordDTO.setTotalDrinkIntake(drinkRecordDTO.getTotalDrinkIntake() + drinkRecord.getWaterIntake());
        }
        drinkRecordDTOS.add(drinkRecordDTO);
        return drinkRecordDTOS;
    }

    private DrinkRecordDetail getDrinkRecordDetail(DrinkRecord drinkRecord, String time) {
        DrinkRecordDetail drinkRecordDetail = new DrinkRecordDetail();
        drinkRecordDetail.setDrinkIntake(drinkRecord.getWaterIntake());
        drinkRecordDetail.setTime(time);
        if (drinkRecord.getImageId() != null) {
            drinkRecordDetail.setImageId(drinkRecord.getImageId());
        }
        return drinkRecordDetail;
    }
}
