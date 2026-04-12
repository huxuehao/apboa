/**
 * 描述：图片渲染处理器
 *
 * 提供增强的图片渲染功能：
 * - 懒加载
 * - 点击预览
 * - 图片标题（figcaption）
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'
import { escapeHtml } from '@/utils/chat/markdown'

/**
 * 图片渲染处理器
 *
 * @param token 图片 Token
 * @returns 渲染后的 HTML
 */
export function imageHandler(token: Tokens.Image): string {
  const { href, title, text } = token
  const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
  const altAttr = text ? ` alt="${escapeHtml(text)}"` : ''

  return `<figure class="md-figure">
    <img src="${href}"${altAttr}${titleAttr} loading="lazy" class="md-image" onclick="window.__openImagePreview__(this)" />
    ${text ? `<figcaption class="md-figcaption">${escapeHtml(text)}</figcaption>` : ''}
  </figure>`
}
