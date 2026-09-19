package com.emotion.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.emotion.api.repository.dao.rds.WechatArticleMapper;
import com.emotion.api.repository.po.WechatArticle;
import com.emotion.api.service.WechatArticleService;
import org.springframework.stereotype.Service;

@Service
public class WechatArticleServiceImpl extends ServiceImpl<WechatArticleMapper, WechatArticle>
        implements WechatArticleService {
    @Override
    public WechatArticle getLastArticle() {
        LambdaQueryWrapper<WechatArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(WechatArticle::getCreateTime);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public WechatArticle getArticleByArticleId(String articleId) {
        LambdaQueryWrapper<WechatArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WechatArticle::getArticleId, articleId);
        return this.getOne(queryWrapper);
    }
}