package com.emotion.api.controller;


import cn.hutool.json.JSONUtil;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.UserRankDTO;
import com.emotion.api.service.IRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rank")
public class RankController {
    @Autowired
    private IRankService rankService;

    @GetMapping("/api/v1/getUserRank")
    public BaseResult<List<UserRankDTO>> getUserRank(@CurrentUser UserPrincipal userPrincipal,
                                                     @RequestParam(defaultValue = "1") Integer type) {
        List<UserRankDTO> userRank = rankService.getUserRank(userPrincipal, type);
        log.info("{} : {}", JSONUtil.toJsonStr(userPrincipal), JSONUtil.toJsonStr(userRank));
        return BaseResult.success(userRank);
    }
}
