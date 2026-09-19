package com.emotion.api.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.emotion.api.dto.DailyMotivationDetailDTO;
import com.emotion.api.dto.DailyMotivationVO;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.service.IDailyMotivationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/daily-motivation")
public class DailyMotivationController {
    @Autowired
    private IDailyMotivationService dailyMotivationService;

    @GetMapping("/api/v1/getMotivation")
    public BaseResult<DailyMotivationVO> getMotivation(@CurrentUser UserPrincipal userPrincipal) {
        BaseResult<DailyMotivationVO> dailyMotivation = dailyMotivationService.getDailyMotivation();
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(dailyMotivation));
        return dailyMotivation;
    }

    @GetMapping("/api/v1/getMotivationList")
    public BaseResult<List<DailyMotivationDetailDTO>> getMotivationList(@CurrentUser UserPrincipal userPrincipal) {
        // 校验用户ID是否为1或10
        if (userPrincipal.getUserId() != 1 && userPrincipal.getUserId() != 10) {
            return BaseResult.error("403", "无权限访问");
        }
        return dailyMotivationService.getMotivationList();
    }

    @PostMapping("/api/v1/updateMotivation")
    public BaseResult<Boolean> updateMotivation(@CurrentUser UserPrincipal userPrincipal, 
                                                @RequestBody DailyMotivationDetailDTO motivationDetailDTO) {
        // 校验用户ID是否为1或10
        if (userPrincipal.getUserId() != 1 && userPrincipal.getUserId() != 10) {
            return BaseResult.error("403", "无权限访问");
        }
        
        if (ObjectUtil.isNull(motivationDetailDTO) || ObjectUtil.isNull(motivationDetailDTO.getId())) {
            return BaseResult.error("500", "鸡汤信息不能为空");
        }
        
        return dailyMotivationService.updateMotivation(motivationDetailDTO);
    }

    @GetMapping("/api/v1/motivationFeedback")
    public BaseResult<Boolean> saveMotivationFeedback(@CurrentUser UserPrincipal userPrincipal, Integer feedbackType) {
        if (ObjectUtil.isNull(userPrincipal) || ObjectUtil.isNull(userPrincipal.getUserId())) {
            return BaseResult.error("500", "用户信息异常");
        }

        if (ObjectUtil.isEmpty(feedbackType)) {
            return BaseResult.error("500", "反馈类型不能为空");
        }

        if (feedbackType != 1 && feedbackType != 2) {
            log.error("无效的反馈类型: {}", feedbackType);
            return BaseResult.error("无效反馈类型！");
        }

        return dailyMotivationService.saveMotivationFeedback(userPrincipal.getUserId(), feedbackType);
    }
}
