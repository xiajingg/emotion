package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishWordQuestions;
import com.emotion.api.repository.dao.rds.EnglishWordQuestionsMapper;
import com.emotion.api.service.IEnglishWordQuestionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储从主文本中抽取的单词问题 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishWordQuestionsServiceImpl extends ServiceImpl<EnglishWordQuestionsMapper, EnglishWordQuestions> implements IEnglishWordQuestionsService {

}
