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

// 容器扩展
export {
  ContainerExtension,
  createContainerExtension,
  containerExtension,
} from './container'
export type {
  ContainerType,
  ContainerTypeConfig,
  ContainerToken,
  ContainerExtensionConfig,
} from './container'
export { defaultContainerConfigs } from './container'

// 渲染器扩展
export {
  createRendererExtension,
  rendererExtension,
  codeHandler,
  linkHandler,
  imageHandler,
  tableHandler,
  headingHandler,
  listitemHandler,
} from './renderer'
