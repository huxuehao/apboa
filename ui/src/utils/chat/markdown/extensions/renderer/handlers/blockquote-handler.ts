/**
 * 描述：引用块渲染处理器
 *
 * 提供引用块渲染功能：
 * - 解析子 tokens
 * - 添加样式类
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 引用块渲染处理器
 *
 * @param token 引用块 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function blockquoteHandler(token: Tokens.Blockquote, parser?: { parse: (tokens: unknown[]) => string }): string {
  // 递归渲染子 tokens
  const content = parser ? parser.parse(token.tokens) : token.text
  return `<blockquote class="md-blockquote">${content}</blockquote>`
}
