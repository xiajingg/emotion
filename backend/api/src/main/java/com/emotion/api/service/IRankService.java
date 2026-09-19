package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.UserRankDTO;
import com.emotion.api.repository.po.UserTextInteraction;

import java.util.List;

public interface IRankService extends IService<UserTextInteraction> {
    List<UserRankDTO> getUserRank(UserPrincipal userPrincipal, Integer type);
}
