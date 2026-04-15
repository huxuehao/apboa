/**
 * 描述：代码块渲染处理器
 *
 * 提供增强的代码块渲染功能，包括：
 * - 语法高亮
 * - 代码复制按钮
 * - 全屏按钮
 * - HTML 代码预览（Tab 切换）
 *
 * @author huxuehao
 **/

import hljs from 'highlight.js'
import type { Tokens } from 'marked'
import {
  escapeHtml,
  isCompleteHtml,
  encodeToBase64,
  generateUniqueId,
} from '@/utils/chat/markdown'
import { decodeFromBase64 } from '@/utils/chat/markdown'

/**
 * 代码块渲染选项
 */
export interface CodeRenderOptions {
  /**
   * 代码内容
   */
  text: string

  /**
   * 语言
   */
  lang?: string

  /**
   * 是否转义 HTML
   * @default true
   */
  escape?: boolean
}

/**
 * 渲染代码高亮
 *
 * @param code 代码内容
 * @param lang 语言
 * @returns 高亮后的 HTML
 */
function highlightCode(code: string, lang?: string): string {
  if (lang && hljs.getLanguage(lang)) {
    return hljs.highlight(code, { language: lang }).value
  }
  return hljs.highlightAuto(code).value
}

/**
 * 生成代码块按钮组
 *
 * @returns 按钮组 HTML
 */
function generateButtonGroup(): string {
  const copyBtn = `<button class="md-code-btn md-code-copy-btn" onclick="window.__copyCodeToClipboard__(this)">复制</button>`
  const fullscreenBtn = `<button class="md-code-btn" title="全屏" onclick="window.__toggleCodeFullscreen__(this)">全屏</button>`
  return `<div class="md-code-btn-group">${copyBtn}${fullscreenBtn}</div>`
}

/**
 * 渲染 HTML 代码块（带预览 Tab）
 *
 * @param text 代码内容
 * @param lang 语言
 * @param highlighted 高亮后的代码
 * @param showPreviewFirst 是否优先显示预览
 * @returns HTML 字符串
 */
function renderHtmlCodeBlock(
  text: string,
  lang: string,
  highlighted: string,
  showPreviewFirst: boolean = false
): string {
  const id = generateUniqueId('code')
  const rawHtmlBase64 = encodeToBase64(text)
  const btnGroup = generateButtonGroup()

  // 根据 showPreviewFirst 决定默认显示的 Tab
  const codeTabClass = showPreviewFirst ? 'md-code-tab' : 'md-code-tab active'
  const previewTabClass = showPreviewFirst ? 'md-code-tab active' : 'md-code-tab'
  const codeViewClass = showPreviewFirst ? 'md-code-view hidden' : 'md-code-view'
  const previewClass = showPreviewFirst ? 'md-code-preview' : 'md-code-preview hidden'

  // 当默认显示预览时，标记 iframe 需要自动加载（绕过 DOMPurify 对 srcdoc 的清理）
  const autoPreviewAttr = showPreviewFirst ? ' data-auto-preview="true"' : ''

  return `<div class="md-code-block md-code-block-html" id="${id}" data-raw-html="${rawHtmlBase64}">
    <div class="md-code-header">
      <div class="md-code-tabs">
        <button class="${previewTabClass}" onclick="window.__toggleHtmlView__(this,'preview')">预览</button>
        <button class="${codeTabClass}" onclick="window.__toggleHtmlView__(this,'code')">代码</button>
      </div>
      ${btnGroup}
    </div>
    <div class="${codeViewClass}">
      <pre><code class="hljs language-${escapeHtml(lang)}">${highlighted}</code></pre>
    </div>
    <div class="${previewClass}">
      <iframe class="md-code-iframe" sandbox="allow-scripts allow-same-origin"${autoPreviewAttr}></iframe>
    </div>
  </div>`
}

/**
 * 渲染普通代码块
 *
 * @param text 代码内容
 * @param lang 语言
 * @param highlighted 高亮后的代码
 * @returns HTML 字符串
 */
function renderNormalCodeBlock(text: string, lang: string, highlighted: string): string {
  const id = generateUniqueId('code')
  const btnGroup = generateButtonGroup()

  return `<div class="md-code-block" id="${id}">
    <div class="md-code-header">
      <span class="md-code-lang">${escapeHtml(lang)}</span>
      ${btnGroup}
    </div>
    <pre><code class="hljs language-${escapeHtml(lang)}">${highlighted}</code></pre>
  </div>`
}

/**
 * 代码块渲染处理器
 *
 * @param token 代码块 Token
 * @returns 渲染后的 HTML
 */
export function codeHandler(token: Tokens.Code): string {
  const { text, lang } = token
  const language = lang || 'text'

  // mermaid代码处理
  if (lang === 'mermaid') {
    return `<pre class="mermaid">${escapeHtml(text)}</pre>`
  }

  const highlighted = highlightCode(text, lang)

  // 判断是否为完整的 HTML 代码
  const isHtml = (lang === 'html' || lang === 'htm') && isCompleteHtml(text)

  if (isHtml) {
    // 完整HTML优先显示预览效果
    return renderHtmlCodeBlock(text, language, highlighted, true)
  }

  return renderNormalCodeBlock(text, language, highlighted)
}
