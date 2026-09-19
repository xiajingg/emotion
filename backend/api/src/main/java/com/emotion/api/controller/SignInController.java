package com.emotion.api.controller;

import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.QuickCheckInDTO;
import com.emotion.api.service.ISignInService;
import com.emotion.api.vo.QuickCheckInVO;
import com.emotion.api.vo.TodayStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sign-in")
public class SignInController {

    @Autowired
    private ISignInService signInService;

    /**
     * 快速打卡
     * @param userPrincipal 当前用户
     * @param dto 打卡请求参数
     * @return 打卡结果
     */
    @PostMapping("/api/v1/quickCheckIn")
    public BaseResult<QuickCheckInVO> quickCheckIn(@CurrentUser UserPrincipal userPrincipal,
                                                    @RequestBody QuickCheckInDTO dto) {
        try {
            // 参数校验
            if (dto.getMoodType() == null || dto.getMoodType() < 1 || dto.getMoodType() > 5) {
                return BaseResult.error("400", "无效的情绪类型");
            }
            if (dto.getMoodEmoji() == null || dto.getMoodEmoji().isEmpty()) {
                return BaseResult.error("400", "表情符号不能为空");
            }

            QuickCheckInVO result = signInService.quickCheckIn(userPrincipal.getUserId(), dto);
            return BaseResult.success(result);
        } catch (RuntimeException e) {
            log.error("快速打卡失败: {}", e.getMessage());
            return BaseResult.error("500", e.getMessage());
        } catch (Exception e) {
            log.error("快速打卡异常", e);
            return BaseResult.error("500", "系统异常");
        }
    }

    /**
     * 查询今日打卡状态
     * @param userPrincipal 当前用户
     * @return 今日状态
     */
    @GetMapping("/api/v1/todayStatus")
    public BaseResult<TodayStatusVO> getTodayStatus(@CurrentUser UserPrincipal userPrincipal) {
        try {
            TodayStatusVO status = signInService.getTodayStatus(userPrincipal.getUserId());
            return BaseResult.success(status);
        } catch (Exception e) {
            log.error("查询今日状态异常", e);
            return BaseResult.error("500", "系统异常");
        }
    }
}
