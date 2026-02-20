/**
 * 公共页面路由
 *
 * @author huxuehao
 */

import type { AppRouteRecordRaw } from '../types'
import { RouteNames, RoutePaths } from '../constants'

/**
 * 业务路由配置
 */
const bizRoutes: AppRouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: RoutePaths.SENSITIVE,
        name: RouteNames.SENSITIVE,
        component: () => import('@/views/Sensitive/index.vue'),
        meta: {
          title: '敏感词',
          hidden: false
        },
      },
      {
        path: RoutePaths.PROMPT,
        name: RouteNames.PROMPT,
        component: () => import('@/views/Prompt/index.vue'),
        meta: {
          title: '系统提示词模版',
          hidden: false
        },
      },
      {
        path: RoutePaths.MODEL,
        name: RouteNames.MODEL,
        component: () => import('@/views/Model/index.vue'),
        meta: {
          title: '模型供应商',
          hidden: false
        },
      },

      {
        path: RoutePaths.AGENT,
        name: RouteNames.AGENT,
        component: () => import('@/views/Agent/index.vue'),
        meta: {
          title: '智能体',
          hidden: false
        },
      },
      {
        path: RoutePaths.HOOK,
        name: RouteNames.HOOK,
        component: () => import('@/views/Hook/index.vue'),
        meta: {
          title: '钩子',
          hidden: false
        },
      },
      {
        path: RoutePaths.TOOL,
        name: RouteNames.TOOL,
        component: () => import('@/views/Tool/index.vue'),
        meta: {
          title: '工具',
          hidden: false
        },
      },
      {
        path: RoutePaths.SKILL,
        name: RouteNames.SKILL,
        component: () => import('@/views/Skill/index.vue'),
        meta: {
          title: '技能包',
          hidden: false
        },
      },
      {
        path: RoutePaths.MCP,
        name: RouteNames.MCP,
        component: () => import('@/views/Mcp/index.vue'),
        meta: {
          title: 'MCP',
          hidden: false
        },
      },
      {
        path: RoutePaths.KNOWLEDGE,
        name: RouteNames.KNOWLEDGE,
        component: () => import('@/views/Knowledge/index.vue'),
        meta: {
          title: '知识库',
          hidden: false
        },
      }
    ]
  }
]

export default bizRoutes
