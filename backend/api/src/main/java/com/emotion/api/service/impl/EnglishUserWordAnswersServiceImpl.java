package com.emotion.api.service.impl;

import com.emotion.api.repository.po.EnglishUserWordAnswers;
import com.emotion.api.repository.dao.rds.EnglishUserWordAnswersMapper;
import com.emotion.api.service.IEnglishUserWordAnswersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 记录用户对单词问题的回答 服务实现类
 * </p>
 *
 * @author xiajing
 * @since 2024-12-20
 */
@Service
public class EnglishUserWordAnswersServiceImpl extends ServiceImpl<EnglishUserWordAnswersMapper, EnglishUserWordAnswers> implements IEnglishUserWordAnswersService {

}
