import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { McpServerDTO, McpServerVO } from '@/types'
import type { McpServer } from '@/types'

/**
 * 分页查询
 * GET /mcp/server/page
 */
export function page(query: McpServerDTO) {
  return request.get<ApiResponse<PageResult<McpServerVO>>>('/api/mcp/server/page', {
    params: query
  })
}

/**
 * 详情
 * GET /mcp/server/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<McpServerVO>>(`/api/mcp/server/${id}`)
}

/**
 * 新增
 * POST /mcp/server
 */
export function save(entity: McpServer) {
  return request.post<ApiResponse<boolean>>('/api/mcp/server', entity)
}

/**
 * 修改
 * PUT /mcp/server
 */
export function update(entity: McpServer) {
  return request.put<ApiResponse<boolean>>('/api/mcp/server', entity)
}

/**
 * 删除
 * DELETE /mcp/server
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/mcp/server', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /mcp/server/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/mcp/server/used-with-agent', ids)
}
