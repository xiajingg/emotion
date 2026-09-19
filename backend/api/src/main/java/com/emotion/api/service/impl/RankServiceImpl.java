package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.dto.UserRankDTO;
import com.emotion.api.repository.dao.rds.UserTextInteractionMapper;
import com.emotion.api.repository.po.UserTextInteraction;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.IRankService;
import com.emotion.api.service.WechatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;

@Service
public class RankServiceImpl extends ServiceImpl<UserTextInteractionMapper, UserTextInteraction> implements IRankService {
    @Autowired
    private UserTextInteractionMapper userTextInteractionMapper;

    @Autowired
    private WechatUserService wechatUserService;
    @Override
    public List<UserRankDTO> getUserRank(UserPrincipal userPrincipal, Integer type) {
        List<UserRankDTO> userRank;
        // 如果类型为2，则返回使用次数排名，否则返回总平均分排名
        if (type == 2) {
            userRank = userTextInteractionMapper.getUserAvgScoreRank(userPrincipal.getUserId());
        } else {
            userRank = userTextInteractionMapper.getUseTotalCountRank(userPrincipal.getUserId());
        }
        int rank = 1;
        for (UserRankDTO userRankDTO : userRank) {
            WechatUser wechatUser = wechatUserService.getById(userRankDTO.getUserName());
            if (userPrincipal.getUserOpenId().equals(wechatUser.getOpenId())) {
                // 当前用户：如果有昵称则显示“昵称（我）”，否则显示“我”
                String nickname = wechatUser.getNickname();
                if (nickname != null && !nickname.trim().isEmpty()) {
                    userRankDTO.setUserName(nickname + "（我）");
                } else {
                    userRankDTO.setUserName("我");
                }
            } else {
                // 其他用户：优先使用昵称，如果没有昵称则使用脱敏的 openId
                String displayName;
                if (wechatUser.getNickname() != null && !wechatUser.getNickname().trim().isEmpty()) {
                    displayName = wechatUser.getNickname();
                } else {
                    String userName = wechatUser.getOpenId();
                    String subFirstName = userName.substring(0, 2);
                    // 截图userId后三位
                    String subLastName = userName.substring(userName.length() - 3);
                    displayName = subFirstName + "***" + subLastName;
                }
                userRankDTO.setUserName(displayName);
            }
            userRankDTO.setRank(rank);
            Float total = userRankDTO.getTotal();
            // total保留一位小数
            userRankDTO.setTotal(Float.valueOf(new DecimalFormat("#.0").format(total)));
            rank++;
        }
        return userRank;
    }
}
