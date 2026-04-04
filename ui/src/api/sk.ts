import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { SecretKeyVO } from '@/types/vo'

/**
 * 查询全部秘钥列表（value已脱敏）
 * GET /sk/list
 */
export function list() {
  return request.get<ApiResponse<SecretKeyVO[]>>('/api/sk/list')
}

/**
 * 新增秘钥
 * POST /sk
 */
export function create(data: Partial<SecretKeyVO>) {
  return request.post<ApiResponse<SecretKeyVO>>('/api/sk', data)
}

/**
 * 更新秘钥（仅允许更新名称）
 * PUT /sk
 */
export function update(data: Pick<SecretKeyVO, 'id' | 'name'>) {
  return request.put<ApiResponse<boolean>>('/api/sk', data)
}

/**
 * 删除秘钥
 * DELETE /sk
 */
export function remove(ids: (string | number)[]) {
  return request.delete<ApiResponse<boolean>>('/api/sk', { data: ids })
}
