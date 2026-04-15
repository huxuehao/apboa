/**
 * 描述：行内代码渲染处理器
 *
 * 提供行内代码渲染功能：
 * - HTML 转义
 * - 添加样式类
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'
import { escapeHtml } from '@/utils/chat/markdown'

/**
 * 行内代码渲染处理器
 *
 * @param token 行内代码 Token
 * @returns 渲染后的 HTML
 */
export function codespanHandler(token: Tokens.Codespan): string {
  return `<code class="md-inline-code">${escapeHtml(token.text)}</code>`
}
