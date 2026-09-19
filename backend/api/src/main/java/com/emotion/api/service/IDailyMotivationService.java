package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.dto.DailyMotivationDetailDTO;
import com.emotion.api.dto.DailyMotivationVO;
import com.emotion.api.config.BaseResult;
import com.emotion.api.repository.po.DailyMotivation;

import java.util.List;

public interface IDailyMotivationService extends IService<DailyMotivation> {
    BaseResult<DailyMotivationVO> getDailyMotivation();

    BaseResult<Boolean> saveMotivationFeedback(Long userId, Integer feedbackType);

    BaseResult<List<DailyMotivationDetailDTO>> getMotivationList();

    BaseResult<Boolean> updateMotivation(DailyMotivationDetailDTO motivationDetailDTO);
}