/**
 * 描述：Markdown 渲染引擎核心类型定义
 *
 * 本文件定义了引擎的所有核心接口和类型，包括：
 * - 扩展接口（Extension）：用于自定义 Markdown 语法
 * - 渲染器处理器接口（RendererHandler）：用于自定义元素渲染
 * - 引擎配置接口（EngineConfig）：引擎初始化配置
 * - Token 类型定义：Marked 相关的类型扩展
 *
 * @author huxuehao
 **/

import type { MarkedExtension, Tokens, Renderer } from 'marked'

/**
 * 扩展级别：block（块级）或 inline（行内）
 */
export type ExtensionLevel = 'block' | 'inline'

/**
 * 自定义 Token 基础接口
 */
export interface CustomToken {
  type: string
  raw: string
  text?: string
}

/**
 * Tokenizer 函数类型定义
 */
export type TokenizerFunction = (
  src: string,
  tokens?: Tokens.Generic[]
) => CustomToken | undefined

/**
 * Renderer 函数类型定义
 */
export type RendererFunction = (token: CustomToken) => string

/**
 * Start 函数类型定义：返回匹配位置或 -1
 */
export type StartFunction = (src: string) => number

/**
 * Markdown 扩展接口
 *
 * 所有自定义 Markdown 语法扩展都应实现此接口。
 * 通过实现此接口，扩展可以注册到引擎中，参与 Markdown 解析和渲染流程。
 *
 * @example
 * ```typescript
 * class MyExtension implements MarkdownExtension {
 *   name = 'myExtension'
 *   level = 'block' as const
 *
 *   start(src: string): number {
 *     return src.indexOf(':::')
 *   }
 *
 *   tokenizer(src: string): CustomToken | undefined {
 *     const match = src.match(/^:::(\w+)\n/)
 *     if (match) {
 *       return {
 *         type: 'myExtension',
 *         raw: match[0],
 *         text: match[1]
 *       }
 *     }
 *   }
 *
 *   renderer(token: CustomToken): string {
 *     return `<div class="custom">${token.text}</div>`
 *   }
 * }
 * ```
 */
export interface MarkdownExtension {
  /**
   * 扩展名称，必须唯一
   */
  readonly name: string

  /**
   * 扩展级别：block（块级）或 inline（行内）
   */
  readonly level: ExtensionLevel

  /**
   * 优先级，数字越小优先级越高，默认为 100
   */
  readonly priority?: number

  /**
   * 定位函数：返回潜在匹配位置
   * @param src 源代码字符串
   * @returns 匹配位置索引，未找到返回 -1
   */
  start: StartFunction

  /**
   * Tokenizer：将源代码转换为 Token
   * @param src 源代码字符串
   * @param tokens 已解析的 tokens（行内扩展使用）
   * @returns 自定义 Token 或 undefined（不匹配时）
   */
  tokenizer: TokenizerFunction

  /**
   * Renderer：将 Token 渲染为 HTML
   * @param token 自定义 Token
   * @returns 渲染后的 HTML 字符串
   */
  renderer: RendererFunction
}

/**
 * 渲染器处理器接口
 *
 * 用于自定义特定 Markdown 元素的渲染行为
 */
export interface RendererHandler<T extends keyof Renderer = keyof Renderer> {
  /**
   * 处理器名称
   */
  readonly name: T

  /**
   * 渲染函数
   */
  render: Renderer[T]
}

/**
 * 引擎配置接口
 */
export interface EngineConfig {
  /**
   * 是否启用 GFM（GitHub Flavored Markdown）
   * @default true
   */
  gfm?: boolean

  /**
   * 是否将换行符转换为 <br>
   * @default true
   */
  breaks?: boolean

  /**
   * 是否启用 XSS 防护（DOMPurify）
   * @default true
   */
  sanitize?: boolean

  /**
   * DOMPurify 配置
   */
  purifyConfig?: Record<string, unknown>

  /**
   * 代码高亮配置
   */
  highlight?: {
    /**
     * 是否启用代码高亮
     * @default true
     */
    enabled?: boolean
  }

  /**
   * 数学公式配置
   */
  math?: {
    /**
     * 是否启用 KaTeX
     * @default true
     */
    enabled?: boolean
  }

  /**
   * 自定义容器配置
   */
  container?: {
    /**
     * 是否启用自定义容器
     * @default true
     */
    enabled?: boolean
  }
}

/**
 * 渲染上下文接口
 *
 * 在渲染过程中传递的上下文信息
 */
export interface RenderContext {
  /**
   * 当前 Marked 解析器实例
   */
  parser: {
    parse: (tokens: Tokens.Generic[]) => string
    parseInline: (tokens: Tokens.Generic[]) => string
  }

  /**
   * 内部渲染函数（用于嵌套解析）
   */
  renderInline: (text: string) => string
}

/**
 * 扩展元数据接口
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
   * 作者信息
   */
  author?: string
}

/**
 * 容器 Token 类型（用于自定义容器扩展）
 */
export interface InfoToken extends CustomToken {
  type: 'container'
  infoType: string
  title: string
}

/**
 * KaTeX Token 类型
 */
export interface KatexToken extends CustomToken {
  type: 'katexBlock' | 'katexInline'
}

/**
 * Marked 扩展适配器类型
 *
 * 用于将 MarkdownExtension 转换为 Marked.MarkedExtension
 */
export type MarkedExtensionAdapter = MarkedExtension
