import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { KnowledgeBaseConfigDTO, KnowledgeBaseConfigVO } from '@/types'
import type { KnowledgeBaseConfig } from '@/types'

/**
 * 分页查询
 * GET /knowledge/config/page
 */
export function page(query: KnowledgeBaseConfigDTO) {
  return request.get<ApiResponse<PageResult<KnowledgeBaseConfigVO>>>(
    '/api/knowledge/config/page',
    { params: query }
  )
}

/**
 * 详情
 * GET /knowledge/config/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<KnowledgeBaseConfigVO>>(`/api/knowledge/config/${id}`)
}

/**
 * 新增
 * POST /knowledge/config
 */
export function save(entity: KnowledgeBaseConfig) {
  return request.post<ApiResponse<boolean>>('/api/knowledge/config', entity)
}

/**
 * 修改
 * PUT /knowledge/config
 */
export function update(entity: KnowledgeBaseConfig) {
  return request.put<ApiResponse<boolean>>('/api/knowledge/config', entity)
}

/**
 * 删除
 * DELETE /knowledge/config
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/knowledge/config', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /knowledge/config/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/knowledge/config/used-with-agent', ids)
}
