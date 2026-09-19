package com.emotion.api.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.dto.OpenIdVO;
import com.emotion.api.config.JwtTokenUtil;
import com.emotion.api.dto.TokenAndIdDTO;
import com.emotion.api.repository.dao.rds.UserFunctionRecordMapper;
import com.emotion.api.repository.dao.rds.UserShareMapper;
import com.emotion.api.repository.dao.rds.WechatUserMapper;
import com.emotion.api.repository.po.UserFunctionRecord;
import com.emotion.api.repository.po.UserShare;
import com.emotion.api.repository.po.WechatUser;
import com.emotion.api.service.WechatClientService;
import com.emotion.api.service.WechatUserService;
import com.emotion.api.util.DingTalkMsg;
import com.emotion.api.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @description 针对表【wechat_user】的数据库操作Service实现
 * @createDate 2024-07-27 15:54:26
 */
@Slf4j
@Service
public class WechatUserServiceImpl extends ServiceImpl<WechatUserMapper, WechatUser>
        implements WechatUserService {

    @Autowired
    private WechatClientService wechatClientService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RedisUtil redisUtil; // 注入 Redis 工具类

    @Autowired
    private UserShareMapper userShareMapper;

    @Autowired
    private UserFunctionRecordMapper userFunctionRecordMapper;

    @Autowired
    private DingTalkMsg dingTalkMsg;

    private static final long TOKEN_EXPIRATION_TIME = 1; // Token 有效期：1天

    @Override
    public String getToken(String code) {
        TokenAndIdDTO tokenAndIdDTO = getTokenAndIdHelper(code);
        return tokenAndIdDTO.getToken();
    }

    @Override
    public synchronized TokenAndIdDTO getTokenAndId(String code) {
        return getTokenAndIdHelper(code);
    }

    @Override
    public WechatUser getUserByOpenId(String openId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<WechatUser>().eq(WechatUser::getOpenId, openId));
    }

    /**
     *
     * @param wechatUserId 是微信的userId
     * @param shareUserId user表的Id
     * @return
     */
    @Override
    public boolean handleShare(String wechatUserId, Long shareUserId) {
        log.info("handleShare: " + wechatUserId + " " + shareUserId);
        // wechatUserId查询user表的id
        WechatUser wechatUser = this.getUserByOpenId(wechatUserId);
        Long userId = wechatUser.getId();
        // 分享链接自己点进来不算分享
        if (userId.equals(shareUserId)) {
            return false;
        }
        // 检查是否是第一次进入
        LambdaQueryWrapper<UserShare> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserShare::getNewUserId, userId);
        UserShare userShare = userShareMapper.selectOne(queryWrapper);
        log.info("userShare: " + userShare);
        // 已经分享过
        if (userShare != null) {
            return false;
        }
        // 记录分享信息
        UserShare newUserShare = new UserShare();
        newUserShare.setNewUserId(userId.toString());
        newUserShare.setShareUserId(shareUserId.toString());
        newUserShare.setCreateTime(LocalDateTime.now());
        userShareMapper.insert(newUserShare);
        log.info("newUserShare:{}", JSONUtil.toJsonStr(newUserShare));
        // 第一次进入，增加微信用户的每日使用次数上限和当天可用次数
        LambdaQueryWrapper<UserFunctionRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<UserFunctionRecord>()
                .eq(UserFunctionRecord::getUserId, shareUserId);
        UserFunctionRecord record = userFunctionRecordMapper.selectOne(recordLambdaQueryWrapper);
        if (record == null) {
            record = new UserFunctionRecord();
            record.setDailyLimitTimes(3L);
            userFunctionRecordMapper.insert(record);
        }
        record.setDailyLimitTimes(record.getDailyLimitTimes() + 1);
        record.setUpdateTime(LocalDateTime.now());
        userFunctionRecordMapper.updateById(record);
        // 钉钉发一条分享成功的消息记录, 包含newUserId,shareUserid和分享时间
        dingTalkMsg.sendMsgToDingTalk("用户分享成功: newUserId:" + userId + " . shareUserId: " + shareUserId + " 分享时间: " + LocalDateTime.now());
        return true;
    }

    @Override
    public Long getIdByToken(String token) {
        String userId = redisUtil.get("JWT_" + token).toString();
        // 传入token过期了直接返回
        if (StrUtil.isEmpty(userId)) {
            return 0L;
        }
        WechatUser user = baseMapper.selectOne(new LambdaQueryWrapper<WechatUser>().eq(WechatUser::getOpenId, userId));
        return user.getId();
    }

    private TokenAndIdDTO getTokenAndIdHelper(String code) {
        // 根据code 获取 openId
        OpenIdVO vo = wechatClientService.getOpenIdByCode(code);
        if (Objects.isNull(vo)) {
            throw new RuntimeException("获取openid异常");
        }
        // 根据openId 生成token
        String openId = vo.getOpenId();
        String token = jwtTokenUtil.generateToken(openId);
        redisUtil.set("JWT_" + token, vo.getOpenId(), TOKEN_EXPIRATION_TIME, TimeUnit.DAYS);

        WechatUser user = baseMapper.selectOne(new LambdaQueryWrapper<WechatUser>().eq(WechatUser::getOpenId, openId));
        if (Objects.isNull(user)) {
            // 保存对象
            user = new WechatUser();
            user.setOpenId(openId);
            user.setSessionKey(vo.getSessionKey());
            user.setCreateTime(LocalDateTime.now());
            baseMapper.insert(user);

            UserFunctionRecord userFunctionRecord = new UserFunctionRecord();
            userFunctionRecord.setUserId(user.getId());
            userFunctionRecord.setDailyLimitTimes(3L);
            userFunctionRecord.setCreateTime(LocalDateTime.now());
            userFunctionRecordMapper.insert(userFunctionRecord);
            dingTalkMsg.sendMsgToDingTalk("新用户加入: userId:" + openId + " . 加入时间: " + LocalDateTime.now());
        }

        TokenAndIdDTO tokenAndIdDTO = new TokenAndIdDTO();
        tokenAndIdDTO.setToken(token);
        tokenAndIdDTO.setId(user.getId());
        return tokenAndIdDTO;
    }
}




