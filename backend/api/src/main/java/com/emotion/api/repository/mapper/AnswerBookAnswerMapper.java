package com.emotion.api.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.AnswerBookAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 答案之书预设答案Mapper接口
 *
 * @author system
 * @since 2026-05-05
 */
@Mapper
public interface AnswerBookAnswerMapper extends BaseMapper<AnswerBookAnswer> {
    
    /**
     * 随机获取一条答案
     * @return 随机答案
     */
    @Select("SELECT * FROM answer_book_answers ORDER BY RAND() LIMIT 1")
    AnswerBookAnswer getRandomAnswer();
}
