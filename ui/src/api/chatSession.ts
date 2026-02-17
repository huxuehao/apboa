import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type {
  ChatSessionCreateDTO,
  ChatMessageAppendDTO,
  ChatSessionQueryDTO,
  ChatSessionVO,
  ChatMessageVO
} from '@/types'

const BASE = '/api/agent/chat/session'

/**
 * 创建新会话
 * POST /agent/chat/session
 */
export function createSession(dto: ChatSessionCreateDTO) {
  return request.post<ApiResponse<ChatSessionVO>>(BASE, dto)
}

/**
 * 正常对话追加消息
 * POST /agent/chat/session/{sessionId}/message
 */
export function appendMessage(sessionId: string, dto: ChatMessageAppendDTO) {
  return request.post<ApiResponse<ChatMessageVO>>(`${BASE}/${sessionId}/message`, dto)
}

/**
 * 重新生成（新分支）
 * POST /agent/chat/session/{sessionId}/regenerate
 */
export function regenerateMessage(sessionId: string, dto: ChatMessageAppendDTO) {
  return request.post<ApiResponse<ChatMessageVO>>(`${BASE}/${sessionId}/regenerate`, dto)
}

/**
 * 切换历史分支
 * PUT /agent/chat/session/{sessionId}/current?messageId=xxx
 */
export function switchCurrentMessage(sessionId: string, messageId: string) {
  return request.put<ApiResponse<unknown>>(`${BASE}/${sessionId}/current`, null, {
    params: { messageId }
  })
}

/**
 * 回显当前完整对话
 * GET /agent/chat/session/{sessionId}/messages/current
 */
export function getCurrentMessages(sessionId: string) {
  return request.get<ApiResponse<ChatMessageVO[]>>(`${BASE}/${sessionId}/messages/current`)
}

/**
 * 会话列表
 * GET /agent/chat/session/list
 */
export function listSessions(query?: ChatSessionQueryDTO) {
  return request.get<ApiResponse<ChatSessionVO[]>>(`${BASE}/list`, {
    params: query
  })
}

/**
 * 会话详情
 * GET /agent/chat/session/{id}
 */
export function getSessionDetail(id: string) {
  return request.get<ApiResponse<ChatSessionVO>>(`${BASE}/${id}`)
}

/**
 * 置顶会话
 * PUT /agent/chat/session/{id}/pin
 */
export function pinSession(id: string) {
  return request.put<ApiResponse<unknown>>(`${BASE}/${id}/pin`)
}

/**
 * 取消置顶会话
 * PUT /agent/chat/session/{id}/unpin
 */
export function unpinSession(id: string) {
  return request.put<ApiResponse<unknown>>(`${BASE}/${id}/unpin`)
}

/**
 * 更新会话标题
 * PUT /agent/chat/session/{id}/title?title=xxx
 */
export function updateSessionTitle(id: string, title: string) {
  return request.put<ApiResponse<unknown>>(`${BASE}/${id}/title`, null, {
    params: { title }
  })
}

/**
 * 删除会话
 * DELETE /agent/chat/session/{id}
 */
export function deleteSession(id: string) {
  return request.delete<ApiResponse<unknown>>(`${BASE}/${id}`)
}
