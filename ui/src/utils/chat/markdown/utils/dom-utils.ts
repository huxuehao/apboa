/**
 * 描述：DOM 操作工具函数集合
 *
 * 提供 HTML 代码块视图切换、全屏、图片预览等 DOM 操作功能
 *
 * @author huxuehao
 **/

import {decodeFromBase64} from './html-utils'
import {ImagePreviewApi} from '@/utils/chat/ImagePreviewApi'

/**
 * 切换 HTML 代码块的显示模式（代码/预览）
 *
 * @param btn 触发切换的按钮元素
 * @param mode 目标模式：'code' 或 'preview'
 */
export function toggleHtmlView(btn: HTMLElement, mode: 'code' | 'preview'): void {
  const codeBlock = btn.closest('.md-code-block') as HTMLElement
  if (!codeBlock) return

  // 更新 Tab 状态
  const tabs = codeBlock.querySelectorAll('.md-code-tab')
  tabs.forEach((tab) => tab.classList.remove('active'))
  btn.classList.add('active')

  // 获取元素
  const codeView = codeBlock.querySelector('.md-code-view')
  const previewView = codeBlock.querySelector('.md-code-preview')

  if (mode === 'code') {
    codeView?.classList.remove('hidden')
    previewView?.classList.add('hidden')
  } else {
    codeView?.classList.add('hidden')
    previewView?.classList.remove('hidden')

    // 首次切换到预览时渲染 iframe
    const iframe = previewView?.querySelector('iframe') as HTMLIFrameElement
    if (iframe && !iframe.dataset.loaded) {
      // 从 data 属性读取 base64 编码的原始 HTML 内容
      const rawHtmlBase64 = codeBlock.dataset.rawHtml
      if (rawHtmlBase64) {
        iframe.srcdoc = decodeFromBase64(rawHtmlBase64)
      }
      iframe.dataset.loaded = 'true'
    }
  }
}

/**
 * 切换代码块的全屏模式
 *
 * @param btn 触发按钮
 */
export function toggleCodeFullscreen(btn: HTMLElement): void {
  const codeBlock = btn.closest('.md-code-block') as HTMLElement
  if (!codeBlock) return

  const isFullscreen = codeBlock.classList.contains('md-code-fullscreen')

  if (isFullscreen) {
    // 退出全屏
    codeBlock.classList.remove('md-code-fullscreen')
    document.body.style.overflow = ''
    // 更新按钮文字
    btn.textContent = '全屏'
    btn.title = '全屏'
  } else {
    // 进入全屏
    codeBlock.classList.add('md-code-fullscreen')
    document.body.style.overflow = 'hidden'
    // 更新按钮文字
    btn.textContent = '退出全屏'
    btn.title = '退出全屏'
  }
}

/**
 * 打开图片预览
 *
 * @param img 图片元素
 */
export function openImagePreview(img: HTMLImageElement): void {
  const src = img.getAttribute('src') || ''
  const alt = img.getAttribute('alt') || '图片预览'
  ImagePreviewApi.open({
    url: src,
    title: alt,
  })
}

/**
 * 复制代码到剪贴板
 *
 * @param btn 触发按钮
 */
export async function copyCodeToClipboard(btn: HTMLElement): Promise<void> {
  const codeBlock = btn.closest('.md-code-block')
  if (!codeBlock) return

  const code = codeBlock.querySelector('code')
  if (!code) return

  const text = code.textContent || ''

  // 保存原始按钮文本，用于恢复
  const originalText = btn.textContent || '复制'

  try {
    // 优先使用 Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      // 降级方案（使用 execCommand，但保持兼容）
      const success = fallbackCopy(text)
      if (!success) throw new Error('降级复制失败')
    }

    // 复制成功反馈
    btn.textContent = '已复制'
    btn.classList.add('copied')
    setTimeout(() => {
      btn.textContent = originalText
      btn.classList.remove('copied')
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
    // 可选：显示错误提示
    btn.textContent = '复制失败'
    btn.classList.add('error')
    setTimeout(() => {
      btn.textContent = originalText
      btn.classList.remove('error')
    }, 2000)
  }
}

// 降级复制函数
function fallbackCopy(text: string): boolean {
  const textarea = document.createElement('textarea')
  textarea.value = text
  // 避免页面滚动
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.opacity = '0'

  document.body.appendChild(textarea)
  textarea.select()
  textarea.setSelectionRange(0, text.length)

  let success = false
  try {
    success = document.execCommand('copy')
  } catch (err) {
    console.warn('execCommand 复制失败:', err)
  }

  document.body.removeChild(textarea)
  return success
}

/**
 * 处理需要自动预览的 HTML 代码块
 *
 * 查找所有标记为 data-auto-preview 的 iframe 并设置 srcdoc
 *
 * @param container DOM 容器元素，默认为 document
 */
export function initAutoPreviewIframes(container: Element | Document = document): void {
  const iframes = container.querySelectorAll<HTMLIFrameElement>('iframe[data-auto-preview="true"]')
  iframes.forEach((iframe) => {
    if (iframe.dataset.loaded) return

    const codeBlock = iframe.closest('.md-code-block') as HTMLElement
    if (!codeBlock) return

    const rawHtmlBase64 = codeBlock.dataset.rawHtml
    if (rawHtmlBase64) {
      iframe.srcdoc = decodeFromBase64(rawHtmlBase64)
      iframe.dataset.loaded = 'true'
    }
  })
}

/**
 * 挂载全局 DOM 操作函数到 window 对象
 *
 * 这些函数供渲染后的 HTML 中的 onclick 属性调用
 */
export function mountGlobalDomHandlers(): void {
  const win = window as unknown as Record<string, unknown>
  win.__toggleHtmlView__ = toggleHtmlView
  win.__toggleCodeFullscreen__ = toggleCodeFullscreen
  win.__openImagePreview__ = openImagePreview
  win.__copyCodeToClipboard__ = copyCodeToClipboard
}
