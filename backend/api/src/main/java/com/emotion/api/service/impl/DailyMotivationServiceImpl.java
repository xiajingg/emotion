package com.emotion.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.dto.DailyMotivationDetailDTO;
import com.emotion.api.dto.DailyMotivationVO;
import com.emotion.api.config.BaseResult;
import com.emotion.api.repository.dao.rds.DailyMotivationMapper;
import com.emotion.api.repository.po.DailyMotivation;
import com.emotion.api.repository.po.MotivationFeedback;
import com.emotion.api.service.IDailyMotivationService;
import com.emotion.api.service.IMotivationFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DailyMotivationServiceImpl extends ServiceImpl<DailyMotivationMapper, DailyMotivation> implements IDailyMotivationService {
    @Autowired
    private DailyMotivationMapper dailyMotivationMapper;

    @Autowired
    private IMotivationFeedbackService motivationFeedbackService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized BaseResult<DailyMotivationVO> getDailyMotivation() {
        // 查询数据库
        List<DailyMotivation> dailyMotivations = getTodayDailyMotivation();

        if (CollUtil.isEmpty(dailyMotivations)) {
            log.error("每日毒鸡汤查询结果为空！");
            return BaseResult.error("每日毒鸡汤查询结果为空！");
        }

        DailyMotivation dailyMotivation = dailyMotivations.get(0);
        String content = dailyMotivation.getContent();

        if (ObjectUtil.isEmpty(content)) {
            log.error("每日毒鸡汤查询结果为空！");
            return BaseResult.error("每日毒鸡汤查询结果为空！");
        }

        DailyMotivationVO dailyMotivationVO = new DailyMotivationVO();
        dailyMotivationVO.setResult(true);
        dailyMotivationVO.setTotalLikes(dailyMotivation.getTotalLikes());
        dailyMotivationVO.setTotalDislikes(dailyMotivation.getTotalDislikes());
        dailyMotivationVO.setMotivationContent(content);

        return BaseResult.success(dailyMotivationVO);
    }

    @Override
    public BaseResult<List<DailyMotivationDetailDTO>> getMotivationList() {
        // 获取当前日期及之后的鸡汤列表
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<DailyMotivation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(DailyMotivation::getDate, today);
        List<DailyMotivation> dailyMotivations = dailyMotivationMapper.selectList(queryWrapper);

        if (CollUtil.isEmpty(dailyMotivations)) {
            log.error("查询结果为空！");
            return BaseResult.error("查询结果为空！");
        }

        List<DailyMotivationDetailDTO> dailyMotivationDetailDTOS = dailyMotivations.stream().map(dailyMotivation -> {
            DailyMotivationDetailDTO dailyMotivationDetailDTO = new DailyMotivationDetailDTO();
            dailyMotivationDetailDTO.setId(dailyMotivation.getId());
            dailyMotivationDetailDTO.setDate(dailyMotivation.getDate());
            dailyMotivationDetailDTO.setMotivationContent(dailyMotivation.getContent());
            return dailyMotivationDetailDTO;
        }).collect(Collectors.toList());

        return BaseResult.success(dailyMotivationDetailDTOS);
    }

    @Override
    public BaseResult<Boolean> updateMotivation(DailyMotivationDetailDTO motivationDetailDTO) {
        if (ObjectUtil.isNull(motivationDetailDTO) || ObjectUtil.isNull(motivationDetailDTO.getId())) {
            return BaseResult.error("500", "鸡汤信息不能为空");
        }

        DailyMotivation dailyMotivation = new DailyMotivation();
        dailyMotivation.setId(motivationDetailDTO.getId());
        dailyMotivation.setContent(motivationDetailDTO.getMotivationContent());
        dailyMotivation.setUpdatedTime(LocalDateTime.now());

        boolean updateResult = this.updateById(dailyMotivation);

        if (updateResult) {
            return BaseResult.success(true);
        } else {
            log.error("更新鸡汤信息失败！");
            return BaseResult.error("更新鸡汤信息失败！");
        }
    }

    @Override
//    @Transactional(rollbackFor = Exception.class)
    public synchronized BaseResult<Boolean> saveMotivationFeedback(Long userId, Integer feedbackType) {
        // 获取当天的毒鸡汤
        List<DailyMotivation> dailyMotivations = getTodayDailyMotivation();

        if (CollUtil.isEmpty(dailyMotivations)) {
            log.error("每日毒鸡汤查询结果为空！");
            return BaseResult.error("每日毒鸡汤查询结果为空！");
        }

        // 查询是否已经评价过
        DailyMotivation dailyMotivation = dailyMotivations.get(0);
//        LambdaQueryWrapper<MotivationFeedback> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(MotivationFeedback::getMotivationId, dailyMotivation.getId());
//        queryWrapper.eq(MotivationFeedback::getUserId, userId);
//        List<MotivationFeedback> list = motivationFeedbackService.list(queryWrapper);
//        if (CollUtil.isNotEmpty(list)) {
//            log.info("已评价！");
//            return BaseResult.error("已评价！");
//        }

        // 没有评价就提交评价
        MotivationFeedback motivationFeedback = new MotivationFeedback();
        motivationFeedback.setMotivationId(dailyMotivation.getId());
        motivationFeedback.setFeedbackType(feedbackType);
        motivationFeedback.setUserId(userId);
        motivationFeedback.setCreatedTime(LocalDateTime.now());
        motivationFeedback.setUpdatedTime(LocalDateTime.now());

        boolean motivationFeedbackSaveResult = motivationFeedbackService.save(motivationFeedback);

        // 然后把点赞或者点踩记录到毒鸡汤表的统计字段
        LambdaUpdateWrapper<DailyMotivation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DailyMotivation::getId, dailyMotivation.getId());
        // 根据 feedbackType 判断更新的字段
        if (feedbackType == 1) {
            // feedbackType == 1，更新 TotalLikes 并更新更新时间
            updateWrapper.set(DailyMotivation::getTotalLikes, dailyMotivation.getTotalLikes() + 1)
                    .set(DailyMotivation::getUpdatedTime, LocalDateTime.now());
        } else {
            // feedbackType == 2，更新 TotalDislikes 并更新更新时间
            updateWrapper.set(DailyMotivation::getTotalDislikes, dailyMotivation.getTotalDislikes() + 1)
                    .set(DailyMotivation::getUpdatedTime, LocalDateTime.now());
        }
        boolean updateResult = this.update(updateWrapper);

        if (motivationFeedbackSaveResult && updateResult) {
            return BaseResult.success(true);
        } else {
            log.error("保存毒鸡汤反馈失败！");
            return BaseResult.error("保存毒鸡汤反馈失败！");
        }

    }

    private List<DailyMotivation> getTodayDailyMotivation() {
        // 获取当前日期
        LocalDate today = LocalDate.now();

        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 查询
        LambdaQueryWrapper<DailyMotivation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyMotivation::getDate, today);

        // 查询数据库
        return dailyMotivationMapper.selectList(queryWrapper);
    }
}
