import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { AgentDefinitionDTO, AgentDefinitionVO } from '@/types'

/**
 * 分页查询
 * GET /agent/definition/page
 */
export function page(query: AgentDefinitionDTO) {
  return request.get<ApiResponse<PageResult<AgentDefinitionVO>>>('/api/agent/definition/page', {
    params: query
  })
}

/**
 * 详情
 * GET /agent/definition/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<AgentDefinitionVO>>(`/api/agent/definition/${id}`)
}

/**
 * 新增
 * POST /agent/definition
 */
export function save(vo: AgentDefinitionVO) {
  return request.post<ApiResponse<boolean>>('/api/agent/definition', vo)
}

/**
 * 修改
 * PUT /agent/definition
 */
export function update(vo: AgentDefinitionVO) {
  return request.put<ApiResponse<boolean>>('/api/agent/definition', vo)
}

/**
 * 删除
 * DELETE /agent/definition
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/agent/definition', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /agent/definition/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/agent/definition/used-with-agent', ids)
}

/**
 * 获取所有Tag
 * GET /api/agent/definition/get/tags
 */
export function listTags() {
  return request.get<ApiResponse<string[]>>('/api/agent/definition/get/tags')
}


/**
 * 执行工具
 */
export function agentDoTool(toolName: string, args: any) {
  return request.post<ApiResponse<any>>(`/api/agent/endpoint/do/${toolName}/tool`, args)
}
