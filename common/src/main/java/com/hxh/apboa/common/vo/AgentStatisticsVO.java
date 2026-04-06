package com.hxh.apboa.common.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 描述：智能体统计分析VO
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
public class AgentStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话数增长趋势 */
    private List<TrendItem> sessionTrend;

    /** 活跃用户数增长趋势 */
    private List<TrendItem> activeUserTrend;

    /** 对话消息数增长趋势 */
    private List<TrendItem> messageTrend;

    /** 平均对话轮次趋势 */
    private List<TrendItem> avgRoundsTrend;

    /**
     * 描述：趋势数据项
     *
     * @author huxuehao
     **/
    @Getter
    @Setter
    @Builder
    public static class TrendItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 日期，格式 yyyy-MM-dd */
        private String date;

        /** 数值 */
        private Double value;
    }
}
