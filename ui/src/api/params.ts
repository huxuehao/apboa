import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { Params } from '@/types'
import type { ToolConfig } from '@/types'

/**
 * 分页查询
 * GET /params/page
 */
export function page(query: Params) {
  return request.get<ApiResponse<PageResult<Params>>>('/api/params/page', { params: query })
}

/**
 * 详情
 * GET /params/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<Params>>(`/api/params/${id}`)
}

/**
 * 新增
 * POST /params
 */
export function save(entity: ToolConfig) {
  return request.post<ApiResponse<boolean>>('/api/params/add', entity)
}

/**
 * 修改
 * POST /params
 */
export function update(entity: ToolConfig) {
  return request.post<ApiResponse<boolean>>('/api/params/update', entity)
}

/**
 * 删除
 * POST /params
 */
export function remove(ids: string[]) {
  return request.post<ApiResponse<boolean>>('/api/params/delete', { data: ids })
}
