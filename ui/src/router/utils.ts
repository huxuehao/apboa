/**
 * 路由工具函数
 *
 * @author huxuehao
 */

import type {RouteLocationNormalized, RouteRecordNormalized} from 'vue-router'
import type {RouteMeta} from './types'

/**
 * 获取所有可见路由（用于菜单）
 */
export function getVisibleRoutes(routes: RouteRecordNormalized[]): RouteRecordNormalized[] {
  return routes.filter((route) => {
    const meta = route.meta as RouteMeta
    if (meta?.hidden) {
      return false
    }
    if (route.children) {
      route.children = getVisibleRoutes(route.children as RouteRecordNormalized[])
    }
    return true
  })
}

/**
 * 生成面包屑
 */
export function generateBreadcrumb(route: RouteLocationNormalized): RouteRecordNormalized[] {
  const breadcrumbs: RouteRecordNormalized[] = []
  const matched = route.matched

  matched.forEach((item) => {
    const meta = item.meta as RouteMeta
    if (meta?.breadcrumb !== false) {
      breadcrumbs.push(item)
    }
  })

  return breadcrumbs
}

/**
 * 查找路由
 */
export function findRoute(
  routes: RouteRecordNormalized[],
  predicate: (route: RouteRecordNormalized) => boolean,
): RouteRecordNormalized | undefined {
  for (const route of routes) {
    if (predicate(route)) {
      return route
    }
    if (route.children) {
      const found = findRoute(route.children as RouteRecordNormalized[], predicate)
      if (found) {
        return found
      }
    }
  }
  return undefined
}

/**
 * 扁平化路由
 */
export function flattenRoutes(routes: RouteRecordNormalized[]): RouteRecordNormalized[] {
  const result: RouteRecordNormalized[] = []

  routes.forEach((route) => {
    result.push(route)
    if (route.children) {
      result.push(...flattenRoutes(route.children as RouteRecordNormalized[]))
    }
  })

  return result
}

/**
 * 重置路由（用于动态路由）
 */
export function resetRouter(router: { matcher: unknown }): void {
  router.matcher = router.matcher
}

/**
 * 判断是否为外链
 */
export function isExternal(path: string): boolean {
  return /^(https?:|mailto:|tel:)/.test(path)
}

/**
 * 解析路径
 */
export function resolvePath(basePath: string, path: string): string {
  if (isExternal(path)) {
    return path
  }
  if (isExternal(basePath)) {
    return basePath
  }
  return basePath + '/' + path
}
