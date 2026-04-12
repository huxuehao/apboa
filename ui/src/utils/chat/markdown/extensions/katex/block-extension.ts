/**
 * 描述：KaTeX 块级公式扩展
 *
 * 支持 $$...$$ 语法的块级数学公式渲染
 *
 * @author huxuehao
 **/

import type { MarkdownExtension, CustomToken } from '../../core/types'
import { renderKatexBlock } from './katex-renderer'

/**
 * KaTeX 块级公式 Token
 */
interface KatexBlockToken extends CustomToken {
  type: 'katexBlock'
  text: string
}

/**
 * KaTeX 块级公式扩展
 *
 * 支持语法：
 * $$
 * E = mc^2
 * $$
 */
export class KatexBlockExtension implements MarkdownExtension {
  readonly name = 'katexBlock'
  readonly level = 'block' as const
  readonly priority = 50

  /**
   * 定位函数：查找 $$ 的位置
   */
  start(src: string): number {
    return src.indexOf('$$')
  }

  /**
   * Tokenizer：解析 $$...$$ 语法
   */
  tokenizer(src: string): KatexBlockToken | undefined {
    const match = src.match(/^\$\$([\s\S]+?)\$\$/)
    if (match) {
      return {
        type: 'katexBlock',
        raw: match[0],
        text: match[1]!.trim(),
      }
    }
    return undefined
  }

  /**
   * Renderer：渲染为 HTML
   */
  renderer(token: CustomToken): string {
    const katexToken = token as KatexBlockToken
    return `<div class="katex-block">${renderKatexBlock(katexToken.text ?? '')}</div>`
  }
}

/**
 * KaTeX 块级公式扩展示例
 *
 * 可直接使用的扩展实例
 */
export const katexBlockExtension = new KatexBlockExtension()
