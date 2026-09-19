package com.emotion.api.dto;

import lombok.Data;

@Data
public class FriendBindStatusVO {
    /**
     * 用户的共享码
     */
    private String shareCode;
    
    /**
     * 是否已绑定好友
     */
    private Boolean isBound;
    
    /**
     * 好友用户ID（如果已绑定）
     */
    private Long friendUserId;
    
    /**
     * 好友昵称（如果已绑定）
     */
    private String friendNickname;
    
    /**
     * 好友OpenID（如果已绑定，用于降级显示）
     */
    private String friendOpenId;
    
    /**
     * ✅ 当前用户昵称
     */
    private String myNickname;
}
