/**
 * 描述：安全工具函数集合
 *
 * 提供 DOMPurify 配置、XSS 防护等安全相关功能
 *
 * @author huxuehao
 **/

import DOMPurify from 'dompurify'

/**
 * 默认 DOMPurify 配置
 *
 * 保留渲染所需的标签和属性，同时防止 XSS 攻击
 */
export const defaultPurifyConfig = {
  ADD_TAGS: [
    'math', 'mrow', 'mi', 'mo', 'mn', 'msup', 'msub', 'mfrac',
    'munderover', 'mover', 'munder', 'msqrt', 'mroot', 'mtable',
    'mtr', 'mtd', 'mtext', 'mspace', 'semantics', 'annotation',
    'figure', 'figcaption', 'iframe',
  ],
  ADD_ATTR: [
    'target', 'rel', 'class', 'id', 'loading', 'onclick',
    'disabled', 'checked', 'type', 'mathvariant', 'encoding',
    'xmlns', 'display', 'columnalign', 'rowalign', 'columnspacing',
    'rowspacing', 'fence', 'stretchy', 'symmetric', 'lspace',
    'rspace', 'accent', 'accentunder', 'scriptlevel', 'movablelimits',
    'separator', 'width', 'height', 'depth', 'voffset', 'style',
    'data-raw-html', 'sandbox', 'srcdoc', 'data-loaded', 'data-auto-preview',
  ],
  ALLOW_DATA_ATTR: false,
}

/**
 * 净化 HTML 内容
 *
 * 使用 DOMPurify 移除潜在的危险内容
 *
 * @param html 原始 HTML 字符串
 * @param config 可选的自定义配置
 * @returns 净化后的安全 HTML
 */
export function sanitizeHtml(
  html: string,
  config: Record<string, unknown> = defaultPurifyConfig
): string {
  return DOMPurify.sanitize(html, config) as string
}

/**
 * 创建自定义净化配置
 *
 * 基于默认配置合并自定义配置
 *
 * @param customConfig 自定义配置
 * @returns 合并后的配置
 */
export function createPurifyConfig(
  customConfig: Record<string, unknown> = {}
): Record<string, unknown> {
  return {
    ...defaultPurifyConfig,
    ...customConfig,
    ADD_TAGS: [
      ...defaultPurifyConfig.ADD_TAGS,
      ...(customConfig.ADD_TAGS as string[] || []),
    ],
    ADD_ATTR: [
      ...defaultPurifyConfig.ADD_ATTR,
      ...(customConfig.ADD_ATTR as string[] || []),
    ],
  }
}
