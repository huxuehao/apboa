import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { SensitiveWordConfigDTO, SensitiveWordConfigVO } from '@/types'
import type { SensitiveWordConfig } from '@/types'

/**
 * 分页查询
 * GET /sensitive/config/page
 */
export function page(query: SensitiveWordConfigDTO) {
  return request.get<ApiResponse<PageResult<SensitiveWordConfigVO>>>(
    '/api/sensitive/config/page',
    { params: query }
  )
}

/**
 * 详情
 * GET /sensitive/config/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<SensitiveWordConfigVO>>(`/api/sensitive/config/${id}`)
}

/**
 * 新增
 * POST /sensitive/config
 */
export function save(entity: SensitiveWordConfig) {
  return request.post<ApiResponse<boolean>>('/api/sensitive/config', entity)
}

/**
 * 修改
 * PUT /sensitive/config
 */
export function update(entity: SensitiveWordConfig) {
  return request.put<ApiResponse<boolean>>('/api/sensitive/config', entity)
}

/**
 * 删除
 * DELETE /sensitive/config
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/sensitive/config', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /sensitive/config/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/sensitive/config/used-with-agent', ids)
}

/**
 * 获取所有分类
 * GET /sensitive/config/get/categories
 */
export function listCategories() {
  return request.get<ApiResponse<string[]>>('/api/sensitive/config/get/categories')
}
