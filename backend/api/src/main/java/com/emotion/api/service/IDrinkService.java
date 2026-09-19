package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.dto.DrinkRecordDTO;
import com.emotion.api.dto.DrinkSubmitDTO;
import com.emotion.api.repository.po.DrinkRecord;

import java.util.List;

/**
 * DrinkImpl
 */
public interface IDrinkService extends IService<DrinkRecord> {
    Integer saveSubmit(Long userId, DrinkSubmitDTO drinkIntake);

    List<DrinkRecordDTO> getDrinkRecordByUserId(Long userId);
}
