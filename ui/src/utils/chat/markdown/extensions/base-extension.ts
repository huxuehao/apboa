/**
 * 描述：Markdown 扩展基类
 *
 * 提供扩展的基础实现，所有自定义扩展可以继承此类
 * 简化扩展的开发流程
 *
 * @author huxuehao
 **/

import type {
  MarkdownExtension,
  ExtensionLevel,
  CustomToken,
  StartFunction,
  TokenizerFunction,
  RendererFunction,
} from '../core/types'

/**
 * 扩展基类
 *
 * 提供扩展的标准实现，子类只需覆盖需要自定义的方法
 *
 * @example
 * ```typescript
 * class MyExtension extends MarkdownExtensionBase {
 *   name = 'myExtension'
 *   level = 'block' as const
 *
 *   start(src: string): number {
 *     return src.indexOf(':::')
 *   }
 *
 *   tokenizer(src: string): CustomToken | undefined {
 *     // 实现 token 解析逻辑
 *   }
 *
 *   renderer(token: CustomToken): string {
 *     // 实现渲染逻辑
 *   }
 * }
 * ```
 */
export abstract class MarkdownExtensionBase implements MarkdownExtension {
  /**
   * 扩展名称，必须唯一
   * 子类必须覆盖此属性
   */
  abstract readonly name: string

  /**
   * 扩展级别：block（块级）或 inline（行内）
   * 子类必须覆盖此属性
   */
  abstract readonly level: ExtensionLevel

  /**
   * 优先级，数字越小优先级越高
   * @default 100
   */
  readonly priority: number = 100

  /**
   * 定位函数：返回潜在匹配位置
   *
   * @param src 源代码字符串
   * @returns 匹配位置索引，未找到返回 -1
   */
  abstract start(src: string): number

  /**
   * Tokenizer：将源代码转换为 Token
   *
   * @param src 源代码字符串
   * @param tokens 已解析的 tokens（行内扩展使用）
   * @returns 自定义 Token 或 undefined（不匹配时）
   */
  abstract tokenizer(src: string, tokens?: unknown[]): CustomToken | undefined

  /**
   * Renderer：将 Token 渲染为 HTML
   *
   * @param token 自定义 Token
   * @returns 渲染后的 HTML 字符串
   */
  abstract renderer(token: CustomToken): string
}

/**
 * 创建扩展的工厂函数
 *
 * 用于快速创建简单的扩展，无需定义类
 *
 * @param config 扩展配置
 * @returns MarkdownExtension 实例
 *
 * @example
 * ```typescript
 * const myExtension = createExtension({
 *   name: 'myExtension',
 *   level: 'block',
 *   start: (src) => src.indexOf(':::'),
 *   tokenizer: (src) => {
 *     const match = src.match(/^:::(\w+)\n/)
 *     if (match) {
 *       return { type: 'myExtension', raw: match[0], text: match[1] }
 *     }
 *   },
 *   renderer: (token) => `<div>${token.text}</div>`
 * })
 * ```
 */
export function createExtension(config: {
  name: string
  level: ExtensionLevel
  priority?: number
  start: StartFunction
  tokenizer: TokenizerFunction
  renderer: RendererFunction
}): MarkdownExtension {
  return {
    name: config.name,
    level: config.level,
    priority: config.priority ?? 100,
    start: config.start,
    tokenizer: config.tokenizer,
    renderer: config.renderer,
  }
}

/**
 * 扩展注册表
 *
 * 管理所有扩展的注册和查询
 */
export class ExtensionRegistry {
  private extensions: Map<string, MarkdownExtension> = new Map()

  /**
   * 注册扩展
   *
   * @param extension 要注册的扩展
   * @throws 如果扩展名称已存在则抛出错误
   */
  register(extension: MarkdownExtension): void {
    if (this.extensions.has(extension.name)) {
      throw new Error(`Extension '${extension.name}' is already registered`)
    }
    this.extensions.set(extension.name, extension)
  }

  /**
   * 取消注册扩展
   *
   * @param name 扩展名称
   * @returns 是否成功取消注册
   */
  unregister(name: string): boolean {
    return this.extensions.delete(name)
  }

  /**
   * 获取扩展
   *
   * @param name 扩展名称
   * @returns 扩展实例或 undefined
   */
  get(name: string): MarkdownExtension | undefined {
    return this.extensions.get(name)
  }

  /**
   * 检查扩展是否已注册
   *
   * @param name 扩展名称
   * @returns 是否已注册
   */
  has(name: string): boolean {
    return this.extensions.has(name)
  }

  /**
   * 获取所有已注册的扩展
   *
   * @returns 扩展数组
   */
  getAll(): MarkdownExtension[] {
    return Array.from(this.extensions.values())
  }

  /**
   * 获取块级扩展
   *
   * @returns 块级扩展数组
   */
  getBlockExtensions(): MarkdownExtension[] {
    return this.getAll()
      .filter((ext) => ext.level === 'block')
      .sort((a, b) => (a.priority ?? 100) - (b.priority ?? 100))
  }

  /**
   * 获取行内扩展
   *
   * @returns 行内扩展数组
   */
  getInlineExtensions(): MarkdownExtension[] {
    return this.getAll()
      .filter((ext) => ext.level === 'inline')
      .sort((a, b) => (a.priority ?? 100) - (b.priority ?? 100))
  }

  /**
   * 清空所有扩展
   */
  clear(): void {
    this.extensions.clear()
  }
}
