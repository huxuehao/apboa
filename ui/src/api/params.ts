import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { Params } from '@/types'

/**
 * 分页查询参数
 */
export interface ParamsPageQuery extends Partial<Params> {
  current?: number
  size?: number
}

/**
 * 分页查询
 * GET /params/page
 */
export function page(query: ParamsPageQuery) {
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
export function save(entity: Params) {
  return request.post<ApiResponse<boolean>>('/api/params/add', entity)
}

/**
 * 修改
 * POST /params/update
 */
export function update(entity: Params) {
  return request.post<ApiResponse<boolean>>('/api/params/update', entity)
}

/**
 * 删除
 * POST /params
 */
export function remove(ids: string[]) {
  return request.post<ApiResponse<boolean>>('/api/params/delete', { data: ids })
}
