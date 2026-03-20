import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { StudioConfig } from '@/types'

/**
 * 分页查询
 * GET /studio/list
 */
export function list() {
  return request.get<ApiResponse<StudioConfig[]>>('/api/studio/list')
}

/**
 * 详情
 * GET /studio/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<StudioConfig>>(`/api/studio/${id}`)
}

/**
 * 新增
 * POST /studio
 */
export function save(entity: StudioConfig) {
  return request.post<ApiResponse<boolean>>('/api/studio', entity)
}

/**
 * 修改
 * PUT /studio
 */
export function update(entity: StudioConfig) {
  return request.put<ApiResponse<boolean>>('/api/studio', entity)
}

/**
 * 删除
 * DELETE /studio
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/studio', { data: ids })
}

/**
 * 被哪些 Agent 使用
 * POST /studio/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/studio/used-with-agent', ids)
}
