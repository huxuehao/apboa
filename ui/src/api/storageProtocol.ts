import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { StorageProtocol } from '@/types'

/**
 * 新增存储协议
 * POST /storage/add
 */
export function add(entity: StorageProtocol) {
  return request.post<ApiResponse<boolean>>('/api/storage/add', entity)
}

/**
 * 编辑存储协议
 * POST /storage/update
 */
export function update(entity: StorageProtocol) {
  return request.post<ApiResponse<boolean>>('/api/storage/update', entity)
}

/**
 * 删除存储协议
 * POST /storage/delete
 */
export function remove(ids: string[]) {
  return request.post<ApiResponse<boolean>>('/api/storage/delete', ids)
}

/**
 * 存储协议分页
 * GET /storage/page
 */
export function page(query: Partial<StorageProtocol> & { current?: number; page?: number; size?: number; sort?: string }) {
  return request.get<ApiResponse<PageResult<StorageProtocol>>>('/api/storage/page', {
    params: query
  })
}

/**
 * 根据ID获取存储协议
 * GET /storage/selectOne
 */
export function selectOne(id: string) {
  return request.get<ApiResponse<StorageProtocol>>('/api/storage/selectOne', {
    params: { id }
  })
}

/**
 * 设置有效
 * GET /storage/validSuccess
 */
export function validSuccess(id: string) {
  return request.get<ApiResponse<boolean>>('/api/storage/validSuccess', {
    params: { id }
  })
}

/**
 * 更新协议配置
 * POST /storage/updateProtocol
 */
export function updateProtocol(entity: Pick<StorageProtocol, 'id' | 'protocolConfig'>) {
  return request.post<ApiResponse<boolean>>('/api/storage/updateProtocol', entity)
}
