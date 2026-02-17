/**
 * 路由配置
 *
 * @author huxuehao
 */

import { createRouter, createWebHashHistory } from 'vue-router'
import type { Router } from 'vue-router'
import { routeModules } from './modules'
import { setupRouterGuard } from './guards'

/**
 * 创建路由实例
 */
const router: Router = createRouter({
  history: createWebHashHistory(),
  routes: routeModules,
  // 滚动行为
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
})

// 设置路由守卫
setupRouterGuard(router)

export default router

// 导出类型和工具
export * from './types'
export * from './constants'
export * from './utils'
export * from './guards'
