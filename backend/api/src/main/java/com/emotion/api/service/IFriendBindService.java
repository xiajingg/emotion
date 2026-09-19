package com.emotion.api.service;

import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.FriendAverageScoreVO;
import com.emotion.api.dto.FriendBindStatusVO;
import com.emotion.api.dto.FriendLinkAnalysisVO;
import com.emotion.api.dto.FriendShareHistoryVO;
import com.emotion.api.dto.FriendTimelineVO;
import com.emotion.api.dto.FriendTrendDataVO;
import com.emotion.api.dto.HistoryEmotion;

public interface IFriendBindService {
    /**
     * 获取或生成用户的共享码
     */
    String getOrCreateShareCode(UserPrincipal userPrincipal);

    /**
     * 绑定好友
     */
    void bindFriend(UserPrincipal userPrincipal, String friendCode);

    /**
     * 获取共享情绪历史（聚合接口，保留兼容）
     */
    FriendShareHistoryVO getFriendShareHistory(UserPrincipal userPrincipal, String friendCode);
    
    /**
     * 获取用户绑定状态
     */
    FriendBindStatusVO getBindStatus(UserPrincipal userPrincipal);
    
    /**
     * ✅ 新增：获取平均分数
     */
    FriendAverageScoreVO getAverageScores(UserPrincipal userPrincipal);
    
    /**
     * ✅ 新增：获取趋势数据
     */
    FriendTrendDataVO getTrendData(UserPrincipal userPrincipal);
    
    /**
     * ✅ 新增：获取时间线记录
     */
    FriendTimelineVO getTimeline(UserPrincipal userPrincipal);

    FriendLinkAnalysisVO getTimelineAnalysis(UserPrincipal userPrincipal);

    HistoryEmotion getTimelineDetail(UserPrincipal userPrincipal, Long id);

    void generateDailyFriendLinkAnalysisForAllBindings();
}
