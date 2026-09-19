package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishUserFullTextTranslations;
import com.emotion.api.repository.dao.rds.EnglishUserFullTextTranslationsMapper;
import com.emotion.api.service.IEnglishUserFullTextTranslationsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 记录用户对整段英文的翻译尝试 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishUserFullTextTranslationsServiceImpl extends ServiceImpl<EnglishUserFullTextTranslationsMapper, EnglishUserFullTextTranslations> implements IEnglishUserFullTextTranslationsService {

}
