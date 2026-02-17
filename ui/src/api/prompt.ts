import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { SystemPromptTemplateDTO, SystemPromptTemplateVO } from '@/types'
import type { SystemPromptTemplate } from '@/types'

/**
 * 分页查询
 * GET /prompt/template/page
 */
export function page(query: SystemPromptTemplateDTO) {
  return request.get<ApiResponse<PageResult<SystemPromptTemplateVO>>>(
    '/api/prompt/template/page',
    { params: query }
  )
}

/**
 * 详情
 * GET /prompt/template/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<SystemPromptTemplateVO>>(`/api/prompt/template/${id}`)
}

/**
 * 新增
 * POST /prompt/template
 */
export function save(entity: SystemPromptTemplate) {
  return request.post<ApiResponse<boolean>>('/api/prompt/template', entity)
}

/**
 * 修改
 * PUT /prompt/template
 */
export function update(entity: SystemPromptTemplate) {
  return request.put<ApiResponse<boolean>>('/api/prompt/template', entity)
}

/**
 * 删除
 * DELETE /prompt/template
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/prompt/template', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /prompt/template/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/prompt/template/used-with-agent', ids)
}

/**
 * 获取所有分类
 * GET /prompt/template/get/categories
 */
export function listCategories() {
  return request.get<ApiResponse<string[]>>('/api/prompt/template/get/categories')
}
