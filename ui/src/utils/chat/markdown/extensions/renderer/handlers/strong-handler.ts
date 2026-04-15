/**
 * 描述：加粗文本渲染处理器
 *
 * 提供加粗文本渲染功能：
 * - 解析子 tokens
 * - 添加 strong 标签
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 加粗文本渲染处理器
 *
 * @param token 加粗文本 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function strongHandler(token: Tokens.Strong, parser?: { parseInline: (tokens: unknown[]) => string }): string {
  // 递归渲染子 tokens 以支持嵌套元素
  const content = parser ? parser.parseInline(token.tokens) : token.text
  return `<strong>${content}</strong>`
}
