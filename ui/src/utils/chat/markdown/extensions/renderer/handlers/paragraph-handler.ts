/**
 * 描述：段落渲染处理器
 *
 * 提供段落渲染功能：
 * - 解析子 tokens
 * - 添加 p 标签
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 段落渲染处理器
 *
 * @param token 段落 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function paragraphHandler(token: Tokens.Paragraph, parser?: { parseInline: (tokens: unknown[]) => string }): string {
  // 递归渲染子 tokens 以支持行内元素
  const content = parser ? parser.parseInline(token.tokens) : token.text
  return `<p>${content}</p>`
}
