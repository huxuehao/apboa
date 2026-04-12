/**
 * 描述：自定义容器扩展类型定义
 *
 * @author huxuehao
 **/

import type { CustomToken } from '../../core/types'

/**
 * 支持的容器类型
 */
export type ContainerType = 'tip' | 'warning' | 'danger' | 'info' | 'success'

/**
 * 容器类型配置
 */
export interface ContainerTypeConfig {
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
export interface ContainerToken extends CustomToken {
  type: 'container'
  containerType: ContainerType
  title: string
}

/**
 * 容器扩展配置
 */
export interface ContainerExtensionConfig {
  /**
   * 启用的容器类型
   * @default ['tip', 'warning', 'danger', 'info', 'success']
   */
  enabledTypes?: ContainerType[]

  /**
   * 自定义类型配置
   */
  typeConfigs?: Partial<Record<ContainerType, Partial<ContainerTypeConfig>>>

  /**
   * 自定义渲染函数
   */
  customRenderer?: (token: ContainerToken) => string
}

/**
 * 默认容器类型配置
 */
export const defaultContainerConfigs: Record<ContainerType, ContainerTypeConfig> = {
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
