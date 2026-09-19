package com.emotion.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.emotion.api.repository.po.WechatArticle;
import org.springframework.stereotype.Service;

@Service
public interface WechatArticleService extends IService<WechatArticle> {

    WechatArticle getLastArticle();

    WechatArticle getArticleByArticleId(String articleId);
}
