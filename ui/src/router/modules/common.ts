/**
 * 公共页面路由
 *
 * @author huxuehao
 */

import type { AppRouteRecordRaw } from '../types'
import { RouteNames, RoutePaths } from '@/router'

/**
 * 公共页面路由配置
 */
const commonRoutes: AppRouteRecordRaw[] = [
  {
    path: RoutePaths.ROOT,
    redirect: RoutePaths.AGENT,
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/Test.vue'),
    meta: {
      title: '测试',
      hidden: true,
    },
  },

  {
    path: `${RoutePaths.CHAT}/:agentId`,
    name: RouteNames.CHAT,
    component: () => import('@/views/Chat/index.vue'),
    meta: {
      title: '对话',
      hidden: true,
    },
  },
  {
    path: `${RoutePaths.CHAT_HISTORY}/:agentId`,
    name: RouteNames.CHAT_HISTORY,
    component: () => import('@/views/ChatHistory/index.vue'),
    meta: {
      title: '对话历史',
      hidden: true,
    },
  },
  {
    path: `${RoutePaths.COMMUNICATION}/:chatKey`,
    name: RouteNames.COMMUNICATION,
    component: () => import('@/views/Communication/index.vue'),
    meta: {
      title: '对话',
      hidden: true,
    },
  },
  {
    path: RoutePaths.PREVIEW,
    name: RouteNames.PREVIEW,
    component: () => import('@/pages/AntdPreview.vue'),
    meta: {
      title: 'AntdPreview',
      hidden: true,
    },
  },
  {
    path: RoutePaths.MARKDOWN_EXTENSION_DOC,
    name: RouteNames.MARKDOWN_EXTENSION_DOC,
    component: () => import('@/views/Doc/MarkdownExtensionDoc.vue'),
    meta: {
      title: 'Markdown 扩展指南',
      hidden: true,
    },
  },
  {
    path: RoutePaths.NOT_FOUND,
    name: RouteNames.NOT_FOUND,
    component: () => import('@/pages/NotFound.vue'),
    meta: {
      title: '页面不存在',
      hidden: true,
    },
  }
]

export default commonRoutes
