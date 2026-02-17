import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { AccountDTO, AccountVO } from '@/types'
import type { Account } from '@/types'
import type { Role } from '@/types'

/**
 * 查询账号列表
 * GET /account/list
 */
export function list(query: AccountDTO) {
  return request.get<ApiResponse<AccountVO[]>>('/api/account/list', { params: query })
}

/**
 * 账号详情
 * GET /account/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<AccountVO>>(`/api/account/${id}`)
}

/**
 * 新增账号
 * POST /account
 */
export function save(entity: Account) {
  return request.post<ApiResponse<boolean>>('/api/account', entity)
}

/**
 * 删除账号
 * DELETE /account
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/account', { data: ids })
}

/**
 * 修改角色
 * PUT /account/{id}/change-role
 */
export function changeRole(id: string, roles: Role[]) {
  return request.put<ApiResponse<boolean>>(`/api/account/${id}/change-role`, roles)
}

/**
 * 禁用/激活用户
 * PUT /account/{id}/toggle-enabled
 */
export function toggleEnabled(id: string, enabled: boolean) {
  return request.put<ApiResponse<boolean>>(`/api/account/${id}/toggle-enabled`, null, {
    params: { enabled }
  })
}

/**
 * 管理员修改用户密码
 * PUT /account/{id}/change-password
 */
export function adminChangePassword(id: string, newPassword: string) {
  return request.put<ApiResponse<boolean>>(`/api/account/${id}/change-password`, null, {
    params: { newPassword }
  })
}
