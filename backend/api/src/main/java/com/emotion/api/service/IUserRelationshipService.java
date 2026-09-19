package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.*;
import com.emotion.api.repository.po.UserRelationship;

import java.util.List;

public interface IUserRelationshipService extends IService<UserRelationship> {
    RelationDTO getUserRelationship(Long userId);

    int bindRelationship(BindUserRequest request, Long bindUserId, UserRelationship userRelationship);

    String saveSubmit(UserPrincipal userPrincipal, SubmitTextDTO text, String result);

    PageResult<TogetherHistory> getTogetherHistory(String code, int page, int size);

    List<SignInHistoryDTO> getTogetherSignInHistory(UserPrincipal userPrincipal, String month, String code);

    UserRelationshipDTO createRelationship(String relation, UserPrincipal userPrincipal);
}
