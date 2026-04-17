/**
 * 描述：自定义容器扩展类型定义
 *
 * @author huxuehao
 **/

import type { MarkdownExtension } from '../../core/types'

// 重新导出 CustomToken 供扩展使用
export type { CustomToken } from '../../core/types'

// 导出info-extension-type所有的类型
export * from './info-extension-type.ts'

/**
 * 扩展模块元数据
 *
 * 用于描述扩展的基本信息
 */
export interface ExtensionMetadata {
  /**
   * 扩展名称
   */
  name: string

  /**
   * 扩展描述
   */
  description?: string

  /**
   * 扩展版本
   */
  version?: string

  /**
   * 扩展作者
   */
  author?: string

  /**
   * 是否默认启用
   * @default true
   */
  enabled?: boolean

  /**
   * 扩展优先级
   */
  priority?: number
}

/**
 * 扩展模块接口
 *
 * 所有扩展模块必须实现此接口才能被自动发现和注册
 * 扩展文件需导出名为 `extensionModule` 的对象
 */
export interface ExtensionModule<T extends MarkdownExtension = MarkdownExtension> {
  /**
   * 扩展元数据
   */
  meta: ExtensionMetadata

  /**
   * 扩展实例
   */
  extension: T

  /**
   * 扩展初始化函数（可选）
   *
   * 在注册到引擎前调用，可用于动态配置
   * @param engine Markdown 引擎实例
   */
  setup?: (engine: unknown) => void | Promise<void>
}

/**
 * 扩展模块导出类型
 *
 * 扩展文件必须默认导出此类型
 */
export type ExtensionModuleExport = ExtensionModule<MarkdownExtension>
