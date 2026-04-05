import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { AgentStatisticsVO } from '@/types'

/**
 * 获取智能体趋势统计数据
 * GET /agent/statistics/{agentId}/trends
 */
export function getAgentTrends(agentId: string, days: number) {
  return request.get<ApiResponse<AgentStatisticsVO>>(`/api/agent/statistics/${agentId}/trends`, {
    params: { days }
  })
}
