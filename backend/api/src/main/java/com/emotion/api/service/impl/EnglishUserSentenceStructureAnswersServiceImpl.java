package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishUserSentenceStructureAnswers;
import com.emotion.api.repository.dao.rds.EnglishUserSentenceStructureAnswersMapper;
import com.emotion.api.service.IEnglishUserSentenceStructureAnswersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 记录用户对句子结构选择题的回答 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishUserSentenceStructureAnswersServiceImpl extends ServiceImpl<EnglishUserSentenceStructureAnswersMapper, EnglishUserSentenceStructureAnswers> implements IEnglishUserSentenceStructureAnswersService {

}
