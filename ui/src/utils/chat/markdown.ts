/**
 * 描述：Markdown 渲染器
 *
 * 使用 API：
 * ```typescript
 * import { renderMarkdown, MarkdownEngine } from '@/utils/chat/markdown'
 * ```
 * @author huxuehao
 **/

export {
  // 核心引擎
  MarkdownEngine,
  createMarkdownEngine,

  // 拓展系统
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
  InfoExtension,
  createInfoExtension,
  infoExtension,

  // 渲染器扩展
  createRendererExtension,
  rendererExtension,
  codeHandler,
  linkHandler,
  imageHandler,
  tableHandler,
  headingHandler,
  listitemHandler,

  // 工具函数
  escapeHtml,
  isCompleteHtml,
  generateUniqueId,
  resetIdCounter,
  encodeToBase64,
  decodeFromBase64,
  generateAnchorId,
  toggleHtmlView,
  toggleCodeFullscreen,
  openImagePreview,
  copyCodeToClipboard,
  mountGlobalDomHandlers,
  sanitizeHtml,
  createPurifyConfig,
  defaultPurifyConfig,

  // 便捷 API
  renderMarkdown,
  renderInlineMarkdown,
} from './markdown/index'

// 导出类型
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
  InfoToken,
  KatexToken,
  MarkedExtensionAdapter,
  KatexRenderOptions,
  InfoType,
  InfoTypeConfig,
  InfoExtensionConfig,
} from './markdown/index'
