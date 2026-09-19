package com.emotion.api.repository.dao.rds;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.repository.po.UserRelationship;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRelationshipMapper extends BaseMapper<UserRelationship> {
}
