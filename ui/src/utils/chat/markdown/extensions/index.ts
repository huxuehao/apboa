/**
 * 描述：扩展模块统一导出
 *
 * @author huxuehao
 **/

// 基础扩展
export {
  MarkdownExtensionBase,
  createExtension,
  ExtensionRegistry,
} from './base-extension'

// KaTeX 扩展
export {
  KatexBlockExtension,
  katexBlockExtension,
  KatexInlineExtension,
  katexInlineExtension,
  renderKatex,
  renderKatexBlock,
  renderKatexInline,
} from './katex'
export type { KatexRenderOptions } from './katex'

// 容器扩展（含自动发现功能）
export {
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
} from './container'
export type {
  ContainerType,
  ContainerTypeConfig,
  ContainerToken,
  ContainerExtensionConfig,
  ExtensionMetadata,
  ExtensionModule,
  ExtensionModuleExport,
} from './container'
export { defaultContainerConfigs } from './container'

// 渲染器扩展（含处理器覆盖机制）
export {
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
} from './renderer'
export type {
  RendererExtensionConfig,
  HandlerConfig,
  HandlerFunctions,
  HandlerName,
  HandlerMetadata,
} from './renderer'
