/**
 * 路由守卫
 *
 * @author huxuehao
 */

import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAccountStore } from '@/stores'
import { WHITE_LIST } from './constants'

// 配置NProgress
NProgress.configure({
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.3,
})

/**
 * 设置页面标题
 */
export function setPageTitle(title?: string): void {
  const defaultTitle = 'Apboa智能体平台'
  if (title) {
    document.title = `${title} - ${defaultTitle}`
  } else {
    document.title = defaultTitle
  }
}

/**
 * 设置路由守卫
 */
export function setupRouterGuard(router: Router): void {
  // 全局前置守卫
  router.beforeEach(async (to, from, next) => {
    // 开启进度条
    NProgress.start()

    // 设置页面标题
    setPageTitle(to.meta?.title as string)

    // 放行“外置对话链接”
    if (to.path.startsWith('/communication/')) {
      next()
      NProgress.done()
      return
    }

    const accountStore = useAccountStore()
    const hasToken = accountStore.isLoggedIn

    // 已登录
    if (hasToken) {
      if (to.path === '/login') {
        // 已登录且访问登录页，重定向到首页
        next({ path: '/' })
        NProgress.done()
      } else {
        // 检查是否已获取用户信息
        if (accountStore.userInfo) {
          next()
        } else {
          try {
            // 获取用户信息
            await accountStore.fetchCurrentUserInfo()
            next()
          } catch (error) {
            // 获取用户信息失败，清除token并重定向到登录页
            await accountStore.logout()
            next({ path: '/login' })
            NProgress.done()
          }
        }
      }
    } else {
      // 未登录
      if (WHITE_LIST.some((path) => to.path === path)) {
        // 在白名单中，直接访问
        next()
      } else {
        // 不在白名单中，重定向到登录页
        next({ path: '/login' })
        NProgress.done()
      }
    }
  })

  // 全局后置守卫
  router.afterEach((to, from, failure) => {
    // 关闭进度条
    NProgress.done()

    // 滚动到顶部
    if (!failure) {
      window.scrollTo(0, 0)
    }
  })

  // 全局错误处理
  router.onError((error) => {
    console.error('路由错误:', error)
    NProgress.done()
  })
}

/**
 * 权限检查
 */
export function hasPermission(roles?: string[], userRoles?: string[]): boolean {
  if (!roles || roles.length === 0) {
    return true
  }
  if (!userRoles || userRoles.length === 0) {
    return false
  }
  return roles.some((role) => userRoles.includes(role))
}
