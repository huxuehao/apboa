/**
 * 账号Store
 *
 * @author huxuehao
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AccountVO, Role } from '@/types'
import * as authApi from '@/api/auth'
import * as accountApi from '@/api/account'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
  UpdateProfileRequest,
} from '@/types'
import {
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
  removeRefreshToken,
  getUser,
  setUser,
  removeUser,
} from '@/utils/auth'

/**
 * 账号Store，管理用户登录状态、信息及相关操作
 */
export const useAccountStore = defineStore('account', () => {
  // ========== State ==========
  /** 当前用户信息 */
  const userInfo = ref<AccountVO | null>(getUser())

  /** 访问令牌 */
  const accessToken = ref<string>(getToken())

  /** 刷新令牌 */
  const refreshToken = ref<string>(getRefreshToken())

  /** 登录状态 */
  const isLoggedIn = computed(() => !!accessToken.value && !!userInfo.value)

  /** 用户角色列表 */
  const roles = computed(() => userInfo.value?.roles || [])

  /** 是否为管理员 */
  const isAdmin = computed(() => roles.value.includes('ADMIN' as Role))

  /** 是否具有编辑权限 */
  const canEdit = computed(() => roles.value.includes('EDIT' as Role) || isAdmin.value)

  /** 是否仅具有只读权限 */
  const isReadOnly = computed(() => roles.value.includes('READ_ONLY' as Role) && !canEdit.value)

  /** 是否刷新页面 */
  const isRefresh = ref<number>(0)

  // ========== Actions ==========

  function setRefresh() {
    isRefresh.value = new Date().getMilliseconds();
  }

  function getRefresh() {
    return isRefresh.value;
  }


  /**
   * 刷新访问凭证
   * @param data
   */
  function setAccessInfo(data: LoginResponse) {
    // 保存token
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    setToken({
      value: data.accessToken,
      ttl: data.accessTokenTTL
    })
    setRefreshToken({
      value: data.refreshToken,
      ttl: data.refreshTokenTTL
    })
  }

  /**
   * 刷新登录
   * @param data
   */
  async function refreshLogin(data: LoginResponse) {
    setAccessInfo(data)

    // 如果需要完整的用户信息，可以调用详情接口
    if (data.userDetail?.id) {
      await fetchCurrentUserInfo(data.userDetail.id)
    }
  }

  /**
   * 用户登录
   * @param loginData 登录信息
   */
  async function login(loginData: LoginRequest): Promise<void> {
    try {
      const response = await authApi.login(loginData)
      const data = response.data.data
      await refreshLogin(data)
    } catch (error) {
      throw error
    }
  }

  /**
   * 用户注册
   * @param registerData 注册信息
   */
  async function register(registerData: RegisterRequest): Promise<boolean> {
    try {
      const response = await authApi.register(registerData)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 刷新Token
   */
  async function refreshAccessToken(): Promise<void> {
    try {
      const response = await authApi.refreshToken(refreshToken.value)
      const data = response.data.data
      await refreshLogin(data)
    } catch (error) {
      // 刷新失败则退出登录
      await logout()
      throw error
    }
  }

  /**
   * 用户退出
   */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch (error) {
      // 即使请求失败也要清除本地数据
      console.error('Logout API failed:', error)
    } finally {
      // 清除所有状态
      clearUserData()
    }
  }

  /**
   * 修改密码
   * @param data 密码修改信息
   */
  async function changePassword(data: ChangePasswordRequest): Promise<boolean> {
    try {
      const response = await authApi.changePassword(data)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 修改个人信息
   * @param data 个人信息
   */
  async function updateProfile(data: UpdateProfileRequest): Promise<boolean> {
    try {
      const response = await authApi.updateProfile(data)
      if (response.data.data && userInfo.value) {
        // 更新本地用户信息
        setUserInfo({
          ...userInfo.value,
          ...data,
        })
      }
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 获取用户详情（通过ID）
   * @param id 用户ID
   */
  async function fetchAccountDetail(id: string): Promise<AccountVO> {
    try {
      const response = await accountApi.detail(id)
      return response.data.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 获取当前用户信息
   * @param userId 用户ID（可选，如果不传则使用当前用户ID）
   */
  async function fetchCurrentUserInfo(userId?: string): Promise<void> {
    const targetId = userId || userInfo.value?.id
    if (!targetId) {
      return
    }
    try {
      const data = await fetchAccountDetail(targetId as string)
      setUserInfo(data)
    } catch (error) {
      throw error
    }
  }

  /**
   * 设置用户信息（用于页面刷新后从localStorage恢复）
   * @param account 用户信息
   */
  function setUserInfo(account: AccountVO): void {
    userInfo.value = account
    setUser(userInfo.value)
  }

  /**
   * 清除用户数据
   */
  function clearUserData(): void {
    userInfo.value = null
    accessToken.value = ''
    refreshToken.value = ''
    removeToken()
    removeRefreshToken()
    removeUser()
  }

  /**
   * 检查是否具有指定角色
   * @param role 角色
   */
  function hasRole(role: Role): boolean {
    return roles.value.includes(role)
  }

  /**
   * 检查是否具有任一角色
   * @param roleList 角色列表
   */
  function hasAnyRole(roleList: Role[]): boolean {
    return roleList.some((role) => roles.value.includes(role))
  }

  /**
   * 检查是否具有所有角色
   * @param roleList 角色列表
   */
  function hasAllRoles(roleList: Role[]): boolean {
    return roleList.every((role) => roles.value.includes(role))
  }

  /**
   * 初始化Store（从localStorage恢复token）
   */
  function initStore(): void {
    const token = getToken()
    const refresh = getRefreshToken()
    const user = getUser()

    if (token) {
      accessToken.value = token
    }
    if (refresh) {
      refreshToken.value = refresh
    }
    if (user) {
      userInfo.value = user
    }
  }

  // 自动初始化
  initStore()

  return {
    // State
    userInfo,
    accessToken,
    refreshToken,
    isLoggedIn,
    roles,
    isAdmin,
    canEdit,
    isReadOnly,

    // Actions
    setRefresh,
    getRefresh,
    login,
    register,
    refreshLogin,
    refreshAccessToken,
    logout,
    changePassword,
    updateProfile,
    fetchAccountDetail,
    fetchCurrentUserInfo,
    setUserInfo,
    setAccessInfo,
    clearUserData,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    initStore,
  }
})
