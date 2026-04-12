/**
 * 描述：自定义容器扩展模块统一导出
 *
 * 提供自动发现和注册机制，新扩展只需在当前目录下创建文件
 * 并导出 extensionModule 对象即可自动被发现
 *
 * @author huxuehao
 **/

import type { MarkdownExtension } from '../../core/types'
import type {
  ExtensionModule,
  ExtensionModuleExport,
  ExtensionMetadata,
} from './types'

// 导出原有内容
export { ContainerExtension, createContainerExtension, containerExtension } from './container-extension'
export type {
  ContainerType,
  ContainerTypeConfig,
  ContainerToken,
  ContainerExtensionConfig,
  ExtensionMetadata,
  ExtensionModule,
  ExtensionModuleExport,
} from './types'
export { defaultContainerConfigs } from './types'

/**
 * 自动发现所有扩展模块
 *
 * 使用 Vite 的 import.meta.glob 扫描当前目录下的扩展文件
 * 文件命名约定：*-extension.ts
 * 自动加载导出了 extensionModule 的模块
 */
const extensionModules = import.meta.glob<ExtensionModuleExport>('./*-extension.ts', {
  eager: true,
  import: 'extensionModule',
})

/**
 * 已发现的扩展模块列表
 */
export const discoveredModules: ExtensionModule[] = Object.values(extensionModules)
  .filter((module): module is ExtensionModule => !!module)

/**
 * 获取所有已启用的扩展
 *
 * @returns 扩展实例数组
 */
export function getEnabledExtensions(): MarkdownExtension[] {
  return discoveredModules
    .filter((module) => module.meta.enabled !== false)
    .sort((a, b) => (a.meta.priority ?? 100) - (b.meta.priority ?? 100))
    .map((module) => module.extension)
}

/**
 * 获取所有已启用的扩展模块
 *
 * @returns 扩展模块数组
 */
export function getEnabledModules(): ExtensionModule[] {
  return discoveredModules
    .filter((module) => module.meta.enabled !== false)
    .sort((a, b) => (a.meta.priority ?? 100) - (b.meta.priority ?? 100))
}

/**
 * 按名称获取扩展模块
 *
 * @param name 扩展名称
 * @returns 扩展模块或 undefined
 */
export function getModuleByName(name: string): ExtensionModule | undefined {
  return discoveredModules.find((module) => module.meta.name === name)
}

/**
 * 批量初始化扩展模块
 *
 * @param engine Markdown 引擎实例
 * @param modules 要初始化的模块列表，默认为所有已启用的模块
 */
export function setupModules(engine: unknown, modules?: ExtensionModule[]): void {
  const targetModules = modules ?? getEnabledModules()
  targetModules.forEach((module) => {
    if (module.setup) {
      module.setup(engine)
    }
  })
}

/**
 * 扩展自动注册器
 *
 * 提供便捷的批量注册功能
 */
export class ExtensionAutoLoader {
  private modules: ExtensionModule[]
  private engine: unknown

  constructor(engine?: unknown) {
    this.modules = getEnabledModules()
    this.engine = engine
  }

  /**
   * 设置引擎实例
   */
  setEngine(engine: unknown): this {
    this.engine = engine
    return this
  }

  /**
   * 获取所有扩展
   */
  getExtensions(): MarkdownExtension[] {
    return this.modules.map((m) => m.extension)
  }

  /**
   * 获取所有模块
   */
  getModules(): ExtensionModule[] {
    return this.modules
  }

  /**
   * 初始化所有模块
   */
  setupAll(): this {
    setupModules(this.engine, this.modules)
    return this
  }

  /**
   * 排除指定扩展
   */
  exclude(...names: string[]): this {
    this.modules = this.modules.filter((m) => !names.includes(m.meta.name))
    return this
  }

  /**
   * 仅包含指定扩展
   */
  only(...names: string[]): this {
    this.modules = this.modules.filter((m) => names.includes(m.meta.name))
    return this
  }
}

/**
 * 创建扩展自动加载器
 *
 * @param engine Markdown 引擎实例
 * @returns ExtensionAutoLoader 实例
 */
export function createAutoLoader(engine?: unknown): ExtensionAutoLoader {
  return new ExtensionAutoLoader(engine)
}
