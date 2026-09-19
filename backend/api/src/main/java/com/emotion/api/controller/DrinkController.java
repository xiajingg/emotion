package com.emotion.api.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.DrinkRecordDTO;
import com.emotion.api.dto.DrinkSubmitDTO;
import com.emotion.api.service.IDrinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/drink")
public class DrinkController {
    @Autowired
    private IDrinkService drinkService;

    /**
     * 提交饮水记录
     *
     * @param userPrincipal
     * @param submitDTO
     * @return
     */
    @PostMapping("/api/v1/submitDrink")
    public BaseResult<Object> submitDrinkRecord(@CurrentUser UserPrincipal userPrincipal, @RequestBody DrinkSubmitDTO submitDTO) {
        if (ObjectUtil.isNull(userPrincipal) || ObjectUtil.isNull(userPrincipal.getUserId())) {
            return BaseResult.error("400", "用户信息异常");
        }
        if (ObjectUtil.isNull(submitDTO) || ObjectUtil.isNull(submitDTO.getDrinkIntake())) {
            return BaseResult.error("400", "参数异常");
        }
        Integer result = drinkService.saveSubmit(userPrincipal.getUserId(), submitDTO);
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), result);
        return BaseResult.success(result);
    }

    @GetMapping("/api/v1/getDrinkRecord")
    public BaseResult<List<DrinkRecordDTO>> getDrinkRecord(@CurrentUser UserPrincipal userPrincipal) {
        if (ObjectUtil.isNull(userPrincipal) || ObjectUtil.isNull(userPrincipal.getUserId())) {
            return BaseResult.error("500", "用户信息异常");
        }
        List<DrinkRecordDTO> drinkSubmitDTOS = drinkService.getDrinkRecordByUserId(userPrincipal.getUserId());
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(drinkSubmitDTOS));
        return BaseResult.success(drinkSubmitDTOS);
    }

}
