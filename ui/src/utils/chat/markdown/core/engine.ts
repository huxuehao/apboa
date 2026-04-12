/**
 * 描述：Markdown 渲染引擎核心类
 *
 * MarkdownEngine 是渲染引擎的核心类，负责：
 * - 管理 Marked 实例
 * - 注册和协调扩展
 * - 提供统一的渲染接口
 * - 处理安全配置
 *
 * @author huxuehao
 **/

import {Marked, type MarkedExtension} from 'marked'
import type {EngineConfig, MarkdownExtension,} from './types'
import {ExtensionRegistry} from '../extensions/base-extension'
import {defaultPurifyConfig, sanitizeHtml} from '../utils/security-utils'
import {resetIdCounter} from '../utils/html-utils'
import {mountGlobalDomHandlers} from '../utils/dom-utils'

/**
 * Markdown 渲染引擎
 *
 * @example
 * ```typescript
 * // 创建引擎实例
 * const engine = new MarkdownEngine()
 *
 * // 注册自定义扩展
 * engine.registerExtension(myExtension)
 *
 * // 渲染 Markdown
 * const html = engine.render('# Hello World')
 * ```
 */
export class MarkdownEngine {
  /**
   * Marked 实例
   */
  private marked: Marked

  /**
   * 扩展注册表
   */
  private extensionRegistry: ExtensionRegistry

  /**
   * 引擎配置
   */
  private config: Required<EngineConfig>

  /**
   * 通过 use 方法添加的 Marked 扩展
   */
  private markedExtensions: MarkedExtension[] = []

  /**
   * 构造函数
   *
   * @param config 引擎配置
   */
  constructor(config: EngineConfig = {}) {
    this.config = this.mergeConfig(config)
    this.marked = new Marked()
    this.extensionRegistry = new ExtensionRegistry()

    // 设置 Marked 选项
    this.marked.setOptions({
      gfm: this.config.gfm,
      breaks: this.config.breaks,
    })

    // 立即挂载全局 DOM 处理器（确保 onclick 事件可用）
    mountGlobalDomHandlers()
  }

  /**
   * 合并配置
   */
  private mergeConfig(config: EngineConfig): Required<EngineConfig> {
    return {
      gfm: config.gfm ?? true,
      breaks: config.breaks ?? true,
      sanitize: config.sanitize ?? true,
      purifyConfig: config.purifyConfig ?? defaultPurifyConfig,
      highlight: {
        enabled: config.highlight?.enabled ?? true,
      },
      math: {
        enabled: config.math?.enabled ?? true,
      },
      container: {
        enabled: config.container?.enabled ?? true,
      },
    }
  }

  /**
   * 将扩展转换为 Marked 扩展格式
   */
  private convertToMarkedExtension(extension: MarkdownExtension): MarkedExtension {
    return {
      extensions: [
        {
          name: extension.name,
          level: extension.level,
          start: extension.start.bind(extension),
          tokenizer: extension.tokenizer.bind(extension),
          renderer: extension.renderer.bind(extension),
        },
      ],
    }
  }

  /**
   * 更新 Marked 实例的扩展
   */
  use(): void {
    // 获取所有扩展并注册
    const extensions = this.extensionRegistry.getAll()

    for (const extension of extensions) {
      const markedExt = this.convertToMarkedExtension(extension)
      this.marked.use(markedExt)
    }
    for (const ext of this.markedExtensions) {
      this.marked.use(ext)
    }
  }

  /**
   * 注册扩展
   *
   * @param extension 要注册的扩展
   * @returns this（链式调用）
   */
  registerExtension(extension: MarkdownExtension): this {
    this.extensionRegistry.register(extension)
    return this
  }

  /**
   * 批量注册扩展
   *
   * @param extensions 扩展数组
   * @returns this（链式调用）
   */
  registerExtensions(extensions: MarkdownExtension[]): this {
    for (const extension of extensions) {
      this.extensionRegistry.register(extension)
    }
    return this
  }

  /**
   * 取消注册扩展
   *
   * @param name 扩展名称
   * @returns this（链式调用）
   */
  unregisterExtension(name: string): this {
    this.extensionRegistry.unregister(name)
    return this
  }

  /**
   * 检查扩展是否已注册
   *
   * @param name 扩展名称
   * @returns 是否已注册
   */
  hasExtension(name: string): boolean {
    return this.extensionRegistry.has(name)
  }

  /**
   * 获取已注册的扩展
   *
   * @param name 扩展名称
   * @returns 扩展实例或 undefined
   */
  getExtension(name: string): MarkdownExtension | undefined {
    return this.extensionRegistry.get(name)
  }

  /**
   * 使用 Marked 扩展
   *
   * @param extension Marked 扩展
   * @returns this（链式调用）
   */
  registerMarkedExtension(extension: MarkedExtension): this {
    this.markedExtensions.push(extension)
    return this
  }

  /**
   * 渲染 Markdown 文本
   *
   * @param text Markdown 原始文本
   * @returns 渲染后的 HTML 字符串
   */
  render(text: string): string {
    if (!text) return ''

    try {
      // 重置 ID 计数器
      resetIdCounter()

      // 渲染 Markdown
      const html = this.marked.parse(text) as string

      // XSS 防护
      if (this.config.sanitize) {
        return sanitizeHtml(html, this.config.purifyConfig)
      }

      return html
    } catch (error) {
      console.error('Markdown render error:', error)
      return this.escapeHtml(text)
    }
  }

  /**
   * 渲染行内 Markdown
   *
   * @param text Markdown 原始文本
   * @returns 渲染后的 HTML 字符串
   */
  renderInline(text: string): string {
    if (!text) return ''

    try {
      const html = this.marked.parseInline(text) as string

      if (this.config.sanitize) {
        return sanitizeHtml(html, this.config.purifyConfig)
      }

      return html
    } catch (error) {
      console.error('Markdown inline render error:', error)
      return this.escapeHtml(text)
    }
  }

  /**
   * HTML 转义
   */
  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
  }

  /**
   * 更新配置
   *
   * @param config 新配置
   * @returns this（链式调用）
   */
  updateConfig(config: Partial<EngineConfig>): this {
    this.config = this.mergeConfig({ ...this.config, ...config })
    this.marked.setOptions({
      gfm: this.config.gfm,
      breaks: this.config.breaks,
    })
    return this
  }

  /**
   * 获取当前配置
   */
  getConfig(): EngineConfig {
    return { ...this.config }
  }
}

/**
 * 创建 Markdown 引擎实例
 *
 * 工厂函数，用于快速创建引擎实例
 *
 * @param config 引擎配置
 * @returns MarkdownEngine 实例
 */
export function createMarkdownEngine(config?: EngineConfig): MarkdownEngine {
  return new MarkdownEngine(config)
}
