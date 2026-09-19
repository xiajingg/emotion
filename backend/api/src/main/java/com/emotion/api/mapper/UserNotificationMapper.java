package com.emotion.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emotion.api.entity.UserNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户消息提醒Mapper
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
    
    /**
     * 统计用户未读消息总数
     */
    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(@Param("userId") String userId);

    /**
     * 统计用户已有消息记录总数（包含已读和未读）
     */
    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") String userId);
    
    /**
     * 统计用户指定类型的未读消息数
     */
    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND notification_type = #{type} AND is_read = 0")
    int countUnreadByType(@Param("userId") String userId, @Param("type") Integer type);
    
    /**
     * 批量插入(忽略重复)
     */
    @Insert("<script>" +
            "INSERT IGNORE INTO user_notification (user_id, notification_type, is_read) VALUES " +
            "<foreach collection='notifications' item='item' separator=','>" +
            "(#{item.userId}, #{item.notificationType}, #{item.isRead})" +
            "</foreach>" +
            "</script>")
    int batchInsertIgnore(@Param("notifications") List<UserNotification> notifications);
}
