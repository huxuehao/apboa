/**
 * 描述：斜体文本渲染处理器
 *
 * 提供斜体文本渲染功能：
 * - 解析子 tokens
 * - 添加 em 标签
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 斜体文本渲染处理器
 *
 * @param token 斜体文本 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function emHandler(token: Tokens.Em, parser?: { parseInline: (tokens: unknown[]) => string }): string {
  // 递归渲染子 tokens 以支持嵌套元素
  const content = parser ? parser.parseInline(token.tokens) : token.text
  return `<em>${content}</em>`
}
