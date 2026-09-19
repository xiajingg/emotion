package com.emotion.api.repository.dao.rds;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.dto.UserRankDTO;
import com.emotion.api.repository.po.UserTextInteraction;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dyz
 * @since 2024-08-01
 */
public interface UserTextInteractionMapper extends BaseMapper<UserTextInteraction> {
    List<UserRankDTO> getUserAvgScoreRank(Long userId);

    List<UserRankDTO> getUseTotalCountRank(Long userId);


}
