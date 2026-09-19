package com.emotion.api.service;

import com.emotion.api.dto.TokenAndIdDTO;
import com.emotion.api.repository.po.WechatUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【wechat_user】的数据库操作Service
* @createDate 2024-07-27 15:54:26
*/
public interface WechatUserService extends IService<WechatUser> {

    String getToken(String code);

    TokenAndIdDTO getTokenAndId(String code);

    WechatUser getUserByOpenId(String openId);

    boolean handleShare(String userId, Long shareUserId);

    Long getIdByToken(String token);
}
