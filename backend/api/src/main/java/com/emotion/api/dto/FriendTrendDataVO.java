package com.emotion.api.dto;

import lombok.Data;
import java.util.List;

/**
 * 好友趋势图数据VO（参考LineChartData格式）
 */
@Data
public class FriendTrendDataVO {
    
    /**
     * 我的趋势数据列表
     */
    private List<TrendDataItem> myData;
    
    /**
     * 好友的趋势数据列表
     */
    private List<TrendDataItem> friendData;
    
    /**
     * 趋势数据项（与LineChartData格式一致）
     */
    @Data
    public static class TrendDataItem {
        /**
         * 分数（-1表示无数据，前端显示虚线）
         */
        private double score;
        
        /**
         * 时间格式：M月d（例如：1月5）
         */
        private String time;
    }
}
