import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { AgentA2A } from '@/types'

/**
 * 保存 A2A 配置
 * POST /agentA2a
 */
export function saveA2aConfig(data: AgentA2A) {
  return request.post<ApiResponse<boolean>>('/api/agentA2a', data)
}

/**
 * 根据 agentId 获取 A2A 配置
 * GET /agentA2a/{agentId}
 */
export function getA2aConfig(agentId: string) {
  return request.get<ApiResponse<AgentA2A>>(`/api/agentA2a/${agentId}`)
}
