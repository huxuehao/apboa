/**
 * 描述：KaTeX 行内公式扩展
 *
 * 支持 $...$ 语法的行内数学公式渲染
 *
 * 注意：需要精确匹配，避免误匹配 Shell 变量如 $HOME, $name 等
 *
 * @author huxuehao
 **/

import type { MarkdownExtension, CustomToken } from '../../core/types'
import { renderKatexInline } from './katex-renderer'

/**
 * KaTeX 行内公式 Token
 */
interface KatexInlineToken extends CustomToken {
  type: 'katexInline'
  text: string
}

/**
 * KaTeX 行内公式扩展
 *
 * 支持语法：$E = mc^2$
 *
 * 匹配规则：
 * - $ 之后不能紧跟数字（避免 $100 被匹配）
 * - 结尾 $ 不能紧跟字母数字
 */
export class KatexInlineExtension implements MarkdownExtension {
  readonly name = 'katexInline'
  readonly level = 'inline' as const
  readonly priority = 50

  /**
   * 定位函数：查找潜在数学公式的起始位置
   *
   * 查找后面跟着非空格和非$字符的$，这是潜在数学公式的开始
   */
  start(src: string): number {
    // 使用正则查找 $ 后面跟着非空白和非 $ 字符的位置
    const match = src.match(/(?<!\\)\$(?=[^\s\$])/)
    return match ? match.index! : -1
  }

  /**
   * Tokenizer：解析 $...$ 语法
   *
   * 匹配规则：
   * - $ 之后不能紧跟数字（避免 $100）
   * - 结尾 $ 不能紧跟字母数字
   */
  tokenizer(src: string): KatexInlineToken | undefined {
    // 修复：要求 $ 之后不能紧跟数字（避免 $100），且结尾 $ 不能紧跟字母数字
    const match = src.match(/^\$([^$\n]+?)\$(?![a-zA-Z0-9])/)
    if (match) {
      return {
        type: 'katexInline',
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
    const katexToken = token as KatexInlineToken
    return renderKatexInline(katexToken.text ?? '')
  }
}

/**
 * KaTeX 行内公式扩展示例
 *
 * 可直接使用的扩展实例
 */
export const katexInlineExtension = new KatexInlineExtension()
