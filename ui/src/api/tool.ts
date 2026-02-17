import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { ToolDTO, ToolVO } from '@/types'
import type { ToolConfig } from '@/types'

/**
 * 分页查询
 * GET /tool/page
 */
export function page(query: ToolDTO) {
  return request.get<ApiResponse<PageResult<ToolVO>>>('/api/tool/page', { params: query })
}

/**
 * 详情
 * GET /tool/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<ToolVO>>(`/api/tool/${id}`)
}

/**
 * 新增
 * POST /tool
 */
export function save(entity: ToolConfig) {
  return request.post<ApiResponse<boolean>>('/api/tool', entity)
}

/**
 * 修改
 * PUT /tool
 */
export function update(entity: ToolConfig) {
  return request.put<ApiResponse<boolean>>('/api/tool', entity)
}

/**
 * 删除
 * DELETE /tool
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/tool', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /tool/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/tool/used-with-agent', ids)
}

/**
 * 获取所有分类
 * GET /api/tool/get/categories
 */
export function listCategories() {
  return request.get<ApiResponse<string[]>>('/api/tool/get/categories')
}
