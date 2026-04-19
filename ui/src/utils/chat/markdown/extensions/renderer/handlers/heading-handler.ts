/**
 * 描述：标题渲染处理器
 *
 * 提供增强的标题渲染功能：
 * - 自动生成锚点 ID
 * - 添加锚点链接
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'
import { generateAnchorId } from '@/utils/chat/markdown'

/**
 * 标题渲染处理器
 *
 * @param token 标题 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function headingHandler(token: Tokens.Heading, parser?: { parseInline: (tokens: unknown[]) => string }): string {
  const { depth, tokens: childTokens } = token
  // 递归渲染子 tokens 以支持行内元素
  const content = parser ? parser.parseInline(childTokens) : token.text
  const anchor = generateAnchorId(token.text)

  return `<h${depth} id="${anchor}" class="md-heading md-h${depth}">${content}</h${depth}>`
  // return `<h${depth} id="${anchor}" class="md-heading md-h${depth}">
  //   <a href="#${anchor}" class="md-heading-anchor">#</a>
  //   ${content}
  // </h${depth}>`
}
