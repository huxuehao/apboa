/**
 * 描述：自定义容器扩展类型定义
 *
 * @author huxuehao
 **/

import type { CustomToken } from '../../core/types'

/**
 * 支持的容器类型
 */
export type InfoType = 'tip' | 'warning' | 'danger' | 'info' | 'success'

/**
 * 容器类型配置
 */
export interface InfoTypeConfig {
  /**
   * 图标
   */
  icon: string

  /**
   * 默认标题
   */
  defaultTitle: string
}

/**
 * 容器 Token
 */
export interface InfoToken extends CustomToken {
  type: 'info'
  infoType: InfoType
  title: string
}

/**
 * 容器扩展配置
 */
export interface InfoExtensionConfig {
  /**
   * 启用的容器类型
   * @default ['tip', 'warning', 'danger', 'info', 'success']
   */
  enabledTypes?: InfoType[]

  /**
   * 自定义类型配置
   */
  typeConfigs?: Partial<Record<InfoType, Partial<InfoTypeConfig>>>

  /**
   * 自定义渲染函数
   */
  customRenderer?: (token: InfoToken) => string
}

/**
 * 默认容器类型配置
 */
export const defaultInfoConfigs: Record<InfoType, InfoTypeConfig> = {
  tip: {
    icon: '💡',
    defaultTitle: '提示',
  },
  warning: {
    icon: '⚠️',
    defaultTitle: '警告',
  },
  danger: {
    icon: '🚨',
    defaultTitle: '危险',
  },
  info: {
    icon: 'ℹ️',
    defaultTitle: '信息',
  },
  success: {
    icon: '✅',
    defaultTitle: '成功',
  },
}
