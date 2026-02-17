import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
  UpdateProfileRequest
} from '@/types'

/**
 * 用户登录
 * POST /auth/login
 */
export function login(data: LoginRequest) {
  return request.post<ApiResponse<LoginResponse>>('/api/auth/login', data)
}

/**
 * 用户注册
 * POST /auth/register
 */
export function register(data: RegisterRequest) {
  return request.post<ApiResponse<boolean>>('/api/auth/register', data)
}

/**
 * 刷新Token
 * POST /auth/refresh-token
 */
export function refreshToken(refreshToken: string) {
  return request.post<ApiResponse<LoginResponse>>('/api/auth/refresh-token', {
    refreshToken
  })
}

/**
 * 用户退出
 * POST /auth/logout
 */
export function logout() {
  return request.post<ApiResponse<void>>('/api/auth/logout')
}

/**
 * 修改密码
 * POST /auth/change-password
 */
export function changePassword(data: ChangePasswordRequest) {
  return request.post<ApiResponse<boolean>>('/api/auth/change-password', data)
}

/**
 * 修改个人信息
 * POST /auth/update-profile
 */
export function updateProfile(data: UpdateProfileRequest) {
  return request.post<ApiResponse<boolean>>('/api/auth/update-profile', data)
}

/**
 * 管理员新增用户
 * POST /auth/admin/create-account
 */
export function adminCreateAccount(data: RegisterRequest) {
  return request.post<ApiResponse<boolean>>('/api/auth/admin/create-account', data)
}
