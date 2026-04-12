/**
 * 描述：KaTeX 公式渲染工具
 *
 * 提供 KaTeX 公式的渲染功能
 *
 * @author huxuehao
 **/

import katex from 'katex'
import { escapeHtml } from '@/utils/chat/markdown'

/**
 * KaTeX 渲染选项
 */
export interface KatexRenderOptions {
  /**
   * 是否块级显示
   */
  displayMode: boolean

  /**
   * 遇到错误时是否抛出
   * @default false
   */
  throwOnError?: boolean

  /**
   * 严格模式
   * @default false
   */
  strict?: boolean

  /**
   * 是否信任输入
   * @default true
   */
  trust?: boolean

  /**
   * 输出格式
   * @default 'html'
   */
  output?: 'html' | 'mathml' | 'htmlAndMathml'
}

/**
 * 默认渲染选项
 */
const defaultOptions: Partial<KatexRenderOptions> = {
  throwOnError: false,
  strict: false,
  trust: true,
  output: 'html',
}

/**
 * 渲染 KaTeX 公式
 *
 * 将数学表达式渲染为 HTML，失败时返回原始文本的错误提示
 *
 * @param expression 数学表达式
 * @param options 渲染选项
 * @returns 渲染后的 HTML
 */
export function renderKatex(
  expression: string,
  options: KatexRenderOptions
): string {
  const mergedOptions = { ...defaultOptions, ...options }

  try {
    return katex.renderToString(expression, mergedOptions)
  } catch {
    return `<code class="katex-error">${escapeHtml(expression)}</code>`
  }
}

/**
 * 渲染块级公式
 *
 * @param expression 数学表达式
 * @returns 渲染后的 HTML
 */
export function renderKatexBlock(expression: string): string {
  return renderKatex(expression, { displayMode: true })
}

/**
 * 渲染行内公式
 *
 * @param expression 数学表达式
 * @returns 渲染后的 HTML
 */
export function renderKatexInline(expression: string): string {
  return renderKatex(expression, { displayMode: false })
}
