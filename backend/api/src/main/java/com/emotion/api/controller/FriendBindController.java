package com.emotion.api.controller;

import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.dto.FriendAverageScoreVO;
import com.emotion.api.dto.FriendBindStatusVO;
import com.emotion.api.dto.FriendLinkAnalysisVO;
import com.emotion.api.dto.FriendShareHistoryVO;
import com.emotion.api.dto.FriendTimelineVO;
import com.emotion.api.dto.FriendTrendDataVO;
import com.emotion.api.dto.HistoryEmotion;
import com.emotion.api.service.IFriendBindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friend")
public class FriendBindController {

    @Autowired
    private IFriendBindService friendBindService;

    @GetMapping("/code")
    public BaseResult<String> getShareCode(@CurrentUser UserPrincipal userPrincipal) {
        String code = friendBindService.getOrCreateShareCode(userPrincipal);
        return BaseResult.success(code);
    }

    @PostMapping("/bind")
    public BaseResult<Void> bindFriend(@CurrentUser UserPrincipal userPrincipal, @RequestParam String code) {
        friendBindService.bindFriend(userPrincipal, code);
        return BaseResult.success(null);
    }

    @GetMapping("/history")
    public BaseResult<FriendShareHistoryVO> getHistory(@CurrentUser UserPrincipal userPrincipal, @RequestParam String code) {
        FriendShareHistoryVO history = friendBindService.getFriendShareHistory(userPrincipal, code);
        return BaseResult.success(history);
    }
    
    @GetMapping("/status")
    public BaseResult<FriendBindStatusVO> getStatus(@CurrentUser UserPrincipal userPrincipal) {
        FriendBindStatusVO status = friendBindService.getBindStatus(userPrincipal);
        return BaseResult.success(status);
    }
    
    /**
     * ✅ 新增：获取平均分数
     */
    @GetMapping("/average-scores")
    public BaseResult<FriendAverageScoreVO> getAverageScores(@CurrentUser UserPrincipal userPrincipal) {
        FriendAverageScoreVO scores = friendBindService.getAverageScores(userPrincipal);
        return BaseResult.success(scores);
    }
    
    /**
     * ✅ 新增：获取趋势数据
     */
    @GetMapping("/trend-data")
    public BaseResult<FriendTrendDataVO> getTrendData(@CurrentUser UserPrincipal userPrincipal) {
        FriendTrendDataVO trendData = friendBindService.getTrendData(userPrincipal);
        return BaseResult.success(trendData);
    }
    
    /**
     * ✅ 新增：获取时间线记录
     */
    @GetMapping("/timeline")
    public BaseResult<FriendTimelineVO> getTimeline(@CurrentUser UserPrincipal userPrincipal) {
        FriendTimelineVO timeline = friendBindService.getTimeline(userPrincipal);
        return BaseResult.success(timeline);
    }

    @GetMapping("/timeline/analysis")
    public BaseResult<FriendLinkAnalysisVO> getTimelineAnalysis(@CurrentUser UserPrincipal userPrincipal) {
        FriendLinkAnalysisVO analysis = friendBindService.getTimelineAnalysis(userPrincipal);
        return BaseResult.success(analysis);
    }

    @GetMapping("/timeline/detail")
    public BaseResult<HistoryEmotion> getTimelineDetail(@CurrentUser UserPrincipal userPrincipal, @RequestParam Long id) {
        HistoryEmotion detail = friendBindService.getTimelineDetail(userPrincipal, id);
        if (detail == null) {
            return BaseResult.error("404", "记录不存在");
        }
        return BaseResult.success(detail);
    }
}
