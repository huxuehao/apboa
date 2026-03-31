import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { CodeExecutionConfig } from '@/types'

/**
 * 列表查询
 * GET /code-execution/list
 */
export function list() {
  return request.get<ApiResponse<CodeExecutionConfig[]>>('/api/code-execution/list')
}

/**
 * 详情
 * GET /code-execution/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<CodeExecutionConfig>>(`/api/code-execution/${id}`)
}

/**
 * 新增
 * POST /code-execution
 */
export function save(entity: CodeExecutionConfig) {
  return request.post<ApiResponse<boolean>>('/api/code-execution', entity)
}

/**
 * 修改
 * PUT /code-execution
 */
export function update(entity: CodeExecutionConfig) {
  return request.put<ApiResponse<boolean>>('/api/code-execution', entity)
}

/**
 * 删除
 * DELETE /code-execution
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/code-execution', { data: ids })
}

/**
 * 被哪些 Agent 使用
 * POST /code-execution/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/code-execution/used-with-agent', ids)
}
