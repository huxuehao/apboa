import request from '@/utils/request'
import type { ApiResponse } from '@/types'

/**
 * 获取智能体对话Key
 * GET /agent/chat-key/{agentId}
 *
 * @param agentId 智能体ID
 * @param refresh 是否刷新Key
 */
export function getChatKey(agentId: string | number, refresh: boolean = false) {
  return request.get<ApiResponse<string>>(`/api/agent/chat-key/${agentId}`, {
    params: { refresh }
  })
}

export function getAgentIdByChatKey(chatKey: string) {
  return request.get<ApiResponse<string>>(`/api/agent/chat-key/${chatKey}/get-agent-id`)
}
