package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishSentenceStructureQuestions;
import com.emotion.api.repository.dao.rds.EnglishSentenceStructureQuestionsMapper;
import com.emotion.api.service.IEnglishSentenceStructureQuestionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储用于句子结构判断的选择题 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishSentenceStructureQuestionsServiceImpl extends ServiceImpl<EnglishSentenceStructureQuestionsMapper, EnglishSentenceStructureQuestions> implements IEnglishSentenceStructureQuestionsService {

}
