/**
 * 路由类型定义
 *
 * @author huxuehao
 */

import type { RouteRecordRaw } from 'vue-router'
import type { Role } from '@/types'

/**
 * 路由元信息
 */
export interface RouteMeta {
  /**
   * 页面标题
   */
  title?: string

  /**
   * 图标
   */
  icon?: string

  /**
   * 是否需要认证
   */
  requiresAuth?: boolean

  /**
   * 角色权限
   */
  roles?: Role[]

  /**
   * 是否在菜单中隐藏
   */
  hidden?: boolean

  /**
   * 是否缓存页面
   */
  keepAlive?: boolean

  /**
   * 面包屑
   */
  breadcrumb?: boolean

  /**
   * 激活的菜单路径（用于侧边栏高亮）
   */
  activeMenu?: string

  /**
   * 是否固定在标签栏
   */
  affix?: boolean

  /**
   * 外链地址
   */
  externalLink?: string
}

/**
 * 扩展的路由配置
 */
export type AppRouteRecordRaw = RouteRecordRaw

/**
 * 路由配置选项
 */
export interface RouterOptions {
  /**
   * 是否启用进度条
   */
  enableProgress?: boolean

  /**
   * 是否启用页面标题
   */
  enableTitle?: boolean

  /**
   * 默认页面标题
   */
  defaultTitle?: string

  /**
   * 标题分隔符
   */
  titleSeparator?: string
}
