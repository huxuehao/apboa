/**
 * 路由模块导出
 *
 * @author huxuehao
 */

import type { AppRouteRecordRaw } from '../types'
import authRoutes from './auth'
import commonRoutes from './common'
import bizRoutes from './biz'

/**
 * 所有路由模块
 */
export const routeModules: AppRouteRecordRaw[] = [
  ...commonRoutes,
  ...authRoutes,
  ...bizRoutes
]

export { authRoutes, commonRoutes, bizRoutes }
