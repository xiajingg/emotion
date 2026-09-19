package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishWechatUser;
import com.emotion.api.repository.dao.rds.EnglishWechatUserMapper;
import com.emotion.api.service.IEnglishWechatUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 微信用户表 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishWechatUserServiceImpl extends ServiceImpl<EnglishWechatUserMapper, EnglishWechatUser> implements IEnglishWechatUserService {

}
