package com.hxh.apboa.agent.service;

import com.hxh.apboa.common.vo.AgentStatisticsVO;

/**
 * 描述：智能体统计分析Service
 *
 * @author huxuehao
 **/
public interface AgentStatisticsService {

    /**
     * 获取智能体趋势统计数据
     *
     * @param agentId 智能体ID
     * @param days    天数范围（3/7/15/30/90）
     * @return 统计数据
     */
    AgentStatisticsVO getAgentTrends(Long agentId, Integer days);
}
