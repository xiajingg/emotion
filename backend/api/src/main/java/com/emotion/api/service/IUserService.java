package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.*;
import com.emotion.api.repository.po.User;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author xiajing
 * @since 2024-03-29
 */
public interface IUserService extends IService<User> {

    User findUserByUserName(String username);

    Long saveSubmit(Long userId, SubmitTextDTO text, String result, Double emotionAvgScore, String aiText);

    PageResult<HistoryEmotion> getUserHistorySubmit(UserPrincipal userPrincipal, int page, int size, int type);

    HistoryEmotion getUserHistorySubmitDetail(UserPrincipal userPrincipal, Long id);

    List<LineChartData> getLineChart(UserPrincipal userPrincipal, int days);

    List<SignInHistoryDTO> getUserSignInHistory(UserPrincipal userPrincipal, String month);

    Integer getRemakeLimit(UserPrincipal userPrincipal);

    UserTextInteractionsAnalysisData getUserTextInteractionsAnalysisData(Long userId, Integer type);

    BaseResult<String> covertImage(Long userId, SubmitTextDTO text);
}
