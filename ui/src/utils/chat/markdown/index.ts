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
 *
 * // 覆盖渲染器处理器
 * import { createRendererExtension } from '@/utils/chat/markdown'
 * const ext = createRendererExtension({
 *   handlers: {
 *     image: (token) => `<img src="${token.href}" />`
 *   }
 * })
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

  // 容器扩展（含自动发现）
  ContainerExtension,
  createContainerExtension,
  containerExtension,
  // 自动发现相关
  discoveredModules,
  getEnabledExtensions,
  getEnabledModules,
  getModuleByName,
  setupModules,
  ExtensionAutoLoader,
  createAutoLoader,

  // 渲染器扩展（含处理器覆盖机制）
  createRendererExtension,
  rendererExtension,
  // 处理器函数
  codeHandler,
  linkHandler,
  imageHandler,
  tableHandler,
  headingHandler,
  listitemHandler,
  // 处理器注册表
  RendererHandlerRegistry,
  globalHandlerRegistry,
} from './extensions'

export type {
  KatexRenderOptions,
  ContainerType,
  ContainerTypeConfig,
  ContainerExtensionConfig,
  ExtensionModule,
  ExtensionModuleExport,
  // 渲染器相关类型
  RendererExtensionConfig,
  HandlerConfig,
  HandlerFunctions,
  HandlerName,
  HandlerMetadata,
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
  initAutoPreviewIframes,

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
  initAutoPreviewIframes,

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
  createAutoLoader,
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

// 使用自动加载器注册容器扩展
const autoLoader = createAutoLoader(defaultEngine)
autoLoader.setupAll() // 初始化所有模块（包括设置内部渲染器）

// 注册所有自动发现的扩展
autoLoader.getExtensions().forEach((ext) => {
  defaultEngine.registerExtension(ext)
})

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
