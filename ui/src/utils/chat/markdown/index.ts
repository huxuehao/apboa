/**
 * 描述：Markdown 渲染引擎统一导出
 *
 * 这是 Markdown 渲染引擎的主入口文件，提供完整的导出：
 * - 核心引擎类和函数
 * - 扩展基类和注册表
 * - 内置扩展（KaTeX、容器、渲染器）
 * - 工具函数
 *
 * @example
 * ```typescript
 * // 基础用法
 * import { renderMarkdown } from '@/utils/chat/markdown'
 * const html = renderMarkdown('# Hello World')
 *
 * // 高级用法：自定义扩展
 * import { MarkdownEngine, createExtension } from '@/utils/chat/markdown'
 *
 * const engine = new MarkdownEngine()
 * engine.registerExtension(createExtension({
 *   name: 'myExtension',
 *   level: 'block',
 *   start: (src) => src.indexOf(':::'),
 *   tokenizer: (src) => { ... },
 *   renderer: (token) => { ... }
 * }))
 * ```
 *
 * @author huxuehao
 **/

// ==================== 核心引擎 ====================
export {
  MarkdownEngine,
  createMarkdownEngine,
} from './core'

export type {
  MarkdownExtension,
  ExtensionLevel,
  CustomToken,
  TokenizerFunction,
  RendererFunction,
  StartFunction,
  RendererHandler,
  EngineConfig,
  RenderContext,
  ExtensionMetadata,
  ContainerToken,
  KatexToken,
  MarkedExtensionAdapter,
} from './core'

// ==================== 扩展系统 ====================
export {
  // 基础扩展
  MarkdownExtensionBase,
  createExtension,
  ExtensionRegistry,

  // KaTeX 扩展
  KatexBlockExtension,
  katexBlockExtension,
  KatexInlineExtension,
  katexInlineExtension,
  renderKatex,
  renderKatexBlock,
  renderKatexInline,

  // 容器扩展
  ContainerExtension,
  createContainerExtension,
  containerExtension,

  // 渲染器扩展
  createRendererExtension,
  rendererExtension,
  codeHandler,
  linkHandler,
  imageHandler,
  tableHandler,
  headingHandler,
  listitemHandler,
} from './extensions'

export type {
  KatexRenderOptions,
  ContainerType,
  ContainerTypeConfig,
  ContainerExtensionConfig,
} from './extensions'

export { defaultContainerConfigs } from './extensions'

// ==================== 工具函数 ====================
import {
  // HTML 工具
  escapeHtml,
  isCompleteHtml,
  generateUniqueId,
  resetIdCounter,
  encodeToBase64,
  decodeFromBase64,
  generateAnchorId,

  // DOM 工具
  toggleHtmlView,
  toggleCodeFullscreen,
  openImagePreview,
  copyCodeToClipboard,
  mountGlobalDomHandlers,

  // 安全工具
  sanitizeHtml,
  createPurifyConfig,
  defaultPurifyConfig,
} from './utils'

export {
  // HTML 工具
  escapeHtml,
  isCompleteHtml,
  generateUniqueId,
  resetIdCounter,
  encodeToBase64,
  decodeFromBase64,
  generateAnchorId,

  // DOM 工具
  toggleHtmlView,
  toggleCodeFullscreen,
  openImagePreview,
  copyCodeToClipboard,
  mountGlobalDomHandlers,

  // 安全工具
  sanitizeHtml,
  createPurifyConfig,
  defaultPurifyConfig,
}

// ==================== 便捷 API ====================

import { MarkdownEngine } from './core'
import {
  katexBlockExtension,
  katexInlineExtension,
  containerExtension,
  rendererExtension,
} from './extensions'

/**
 * 默认引擎实例
 *
 * 预配置了所有内置扩展的引擎实例
 */
const defaultEngine = new MarkdownEngine()

// 注册内置扩展
defaultEngine
  .registerMarkedExtension(rendererExtension)
  .registerExtension(katexBlockExtension)
  .registerExtension(katexInlineExtension)

// 注册容器扩展（需要特殊处理内部渲染）
const containerExt = containerExtension
containerExt.setInnerRender((text) => defaultEngine.render(text))
defaultEngine.registerExtension(containerExt)

// 刷新引擎，确保 use() 添加的 rendererExtension 被正确应用
defaultEngine.use()

/**
 * 渲染 Markdown 文本
 *
 * 使用默认引擎实例渲染 Markdown
 *
 * @param text Markdown 原始文本
 * @returns 渲染后的安全 HTML 字符串
 *
 * @example
 * ```typescript
 * import { renderMarkdown } from '@/utils/chat/markdown'
 *
 * const html = renderMarkdown('# Hello\n\nThis is **bold** text.')
 * // 返回: <h1 id="hello">...</h1><p>This is <strong>bold</strong> text.</p>
 * ```
 */
export function renderMarkdown(text: string): string {
  return defaultEngine.render(text)
}

/**
 * 渲染行内 Markdown
 *
 * @param text Markdown 原始文本
 * @returns 渲染后的 HTML 字符串
 */
export function renderInlineMarkdown(text: string): string {
  return defaultEngine.renderInline(text)
}
