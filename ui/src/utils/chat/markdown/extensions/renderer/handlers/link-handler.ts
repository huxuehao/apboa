/**
 * 描述：链接渲染处理器
 *
 * 提供增强的链接渲染功能：
 * - 外链新窗口打开
 * - 添加安全属性（noopener noreferrer）
 * - 外链图标
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'
import { escapeHtml } from '@/utils/chat/markdown'

/**
 * 外链图标 SVG
 */
const EXTERNAL_ICON = `<svg class="md-external-icon" viewBox="0 0 24 24" width="12" height="12"><path fill="currentColor" d="M14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3m-2 16H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2v7Z"/></svg>`

/**
 * 检查是否为外部链接
 *
 * @param href 链接地址
 * @returns 是否为外部链接
 */
function isExternalLink(href: string): boolean {
  return href.startsWith('http://') || href.startsWith('https://')
}

/**
 * 链接渲染处理器
 *
 * @param token 链接 Token
 * @param parser Marked 解析器，用于渲染子 tokens
 * @returns 渲染后的 HTML
 */
export function linkHandler(token: Tokens.Link, parser?: { parseInline: (tokens: unknown[]) => string }): string {
  const { href, title, tokens: childTokens } = token
  // 递归渲染子 tokens 以支持行内元素（如链接中的加粗文本）
  const content = parser ? parser.parseInline(childTokens) : token.text
  const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
  const isExternal = isExternalLink(href)
  const externalAttrs = isExternal ? ' target="_blank" rel="noopener noreferrer"' : ''
  const externalIcon = isExternal ? EXTERNAL_ICON : ''

  return `<a href="${href}"${titleAttr}${externalAttrs} class="md-link">${content}${externalIcon}</a>`
}
