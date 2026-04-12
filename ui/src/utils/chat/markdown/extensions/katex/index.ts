/**
 * 描述：KaTeX 扩展模块统一导出
 *
 * 提供 KaTeX 数学公式渲染的块级和行内扩展
 *
 * @author huxuehao
 **/

export { KatexBlockExtension, katexBlockExtension } from './block-extension'
export { KatexInlineExtension, katexInlineExtension } from './inline-extension'
export { renderKatex, renderKatexBlock, renderKatexInline } from './katex-renderer'
export type { KatexRenderOptions } from './katex-renderer'
