import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { HookConfigDTO, HookConfigVO } from '@/types'
import type { HookConfig } from '@/types'

/**
 * 分页查询
 * GET /hook-config/page
 */
export function page(query: HookConfigDTO) {
  return request.get<ApiResponse<PageResult<HookConfigVO>>>('/api/hook-config/page', {
    params: query
  })
}

/**
 * 详情
 * GET /hook-config/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<HookConfigVO>>(`/api/hook-config/${id}`)
}

/**
 * 新增
 * POST /hook-config
 */
export function save(entity: HookConfig) {
  return request.post<ApiResponse<boolean>>('/api/hook-config', entity)
}

/**
 * 修改
 * PUT /hook-config
 */
export function update(entity: HookConfig) {
  return request.put<ApiResponse<boolean>>('/api/hook-config', entity)
}

/**
 * 删除
 * DELETE /hook-config
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/hook-config', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /hook-config/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/hook-config/used-with-agent', ids)
}
