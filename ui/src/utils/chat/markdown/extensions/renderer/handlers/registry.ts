/**
 * 描述：渲染处理器注册表
 *
 * 管理所有渲染处理器，支持覆盖和扩展
 *
 * @author huxuehao
 **/

import type { Tokens, Renderer } from 'marked'

/**
 * 处理器函数类型映射
 *
 * 定义每个处理器对应的函数签名
 */
export interface HandlerFunctions {
  code: (token: Tokens.Code, parser?: unknown) => string
  codespan: (token: Tokens.Codespan) => string
  link: (token: Tokens.Link, parser?: unknown) => string
  image: (token: Tokens.Image) => string
  table: (token: Tokens.Table, parser?: unknown) => string
  heading: (token: Tokens.Heading, parser?: unknown) => string
  list: (token: Tokens.List, parser?: unknown) => string
  listitem: (token: Tokens.ListItem, parser?: unknown) => string
  strong: (token: Tokens.Strong, parser?: unknown) => string
  em: (token: Tokens.Em, parser?: unknown) => string
  blockquote: (token: Tokens.Blockquote, parser?: unknown) => string
  hr: () => string
  paragraph: (token: Tokens.Paragraph, parser?: unknown) => string
  br: () => string
}

/**
 * 处理器名称类型
 */
export type HandlerName = keyof HandlerFunctions

/**
 * 处理器配置
 */
export type HandlerConfig<K extends HandlerName = HandlerName> = {
  [P in K]?: HandlerFunctions[P]
}

/**
 * 处理器元数据
 */
export interface HandlerMetadata {
  /**
   * 处理器名称
   */
  name: HandlerName

  /**
   * 处理器描述
   */
  description?: string

  /**
   * 是否为默认处理器
   */
  isDefault?: boolean
}

/**
 * 注册的处理器条目
 */
interface RegisteredHandler<K extends HandlerName = HandlerName> {
  meta: HandlerMetadata
  handler: HandlerFunctions[K]
}

/**
 * 渲染处理器注册表
 *
 * 管理所有渲染处理器，支持：
 * - 注册自定义处理器
 * - 覆盖默认处理器
 * - 获取处理器
 */
export class RendererHandlerRegistry {
  private handlers: Map<HandlerName, RegisteredHandler> = new Map()

  /**
   * 注册处理器
   *
   * @param name 处理器名称
   * @param handler 处理器函数
   * @param meta 处理器元数据
   */
  register<K extends HandlerName>(
    name: K,
    handler: HandlerFunctions[K],
    meta?: Partial<HandlerMetadata>
  ): this {
    this.handlers.set(name, {
      meta: {
        name,
        isDefault: meta?.isDefault ?? false,
        ...meta,
      },
      handler,
    })
    return this
  }

  /**
   * 注销处理器
   *
   * @param name 处理器名称
   */
  unregister(name: HandlerName): boolean {
    return this.handlers.delete(name)
  }

  /**
   * 获取处理器
   *
   * @param name 处理器名称
   * @returns 处理器函数或 undefined
   */
  get<K extends HandlerName>(name: K): HandlerFunctions[K] | undefined {
    return this.handlers.get(name)?.handler as HandlerFunctions[K] | undefined
  }

  /**
   * 检查处理器是否存在
   *
   * @param name 处理器名称
   */
  has(name: HandlerName): boolean {
    return this.handlers.has(name)
  }

  /**
   * 批量注册处理器
   *
   * @param config 处理器配置对象
   * @param meta 元数据（应用于所有处理器）
   */
  registerAll(config: HandlerConfig, meta?: Partial<HandlerMetadata>): this {
    for (const [name, handler] of Object.entries(config)) {
      if (handler) {
        this.register(name as HandlerName, handler as HandlerFunctions[HandlerName], meta)
      }
    }
    return this
  }

  /**
   * 获取所有已注册的处理器名称
   */
  getNames(): HandlerName[] {
    return Array.from(this.handlers.keys())
  }

  /**
   * 获取处理器元数据
   *
   * @param name 处理器名称
   */
  getMetadata(name: HandlerName): HandlerMetadata | undefined {
    return this.handlers.get(name)?.meta
  }

  /**
   * 清空所有处理器
   */
  clear(): void {
    this.handlers.clear()
  }

  /**
   * 创建处理器配置对象
   *
   * 用于 createRendererExtension
   */
  toConfig(): HandlerConfig {
    const config: HandlerConfig = {}
    for (const [name, entry] of this.handlers) {
      (config as Record<string, unknown>)[name] = entry.handler
    }
    return config
  }

  /**
   * 克隆注册表
   */
  clone(): RendererHandlerRegistry {
    const cloned = new RendererHandlerRegistry()
    for (const [name, entry] of this.handlers) {
      cloned.handlers.set(name, { ...entry })
    }
    return cloned
  }
}

/**
 * 全局处理器注册表实例
 */
export const globalHandlerRegistry = new RendererHandlerRegistry()
