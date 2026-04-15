/**
 * 描述：HTML 工具函数集合
 *
 * 提供 HTML 转义、HTML 完整性检测等工具函数
 *
 * @author huxuehao
 **/

/**
 * HTML 转义，防止 XSS 攻击
 *
 * 将特殊字符转换为 HTML 实体，防止恶意脚本注入
 *
 * @param str 需要转义的字符串
 * @returns 转义后的安全字符串
 */
export function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 判断是否为完整的 HTML 代码
 *
 * @param code 代码内容
 * @returns 是否为完整的 HTML
 */
export function isCompleteHtml(code: string): boolean {
  const normalizedCode = code.trim()

  if (!normalizedCode) return false

  // 标准完整 HTML 结构
  const hasCompleteStructure = /<html[\s>][\s\S]*<\/html\s*>/i.test(normalizedCode) &&
    /<head[\s>][\s\S]*<\/head\s*>/i.test(normalizedCode) &&
    /<body[\s>][\s\S]*<\/body\s*>/i.test(normalizedCode)

  // 至少有 body 标签（最简可用）
  const hasBody = /<body[\s>][\s\S]*<\/body\s*>/i.test(normalizedCode)

  // 或者有完整的 html/head/body 结构
  return hasBody || hasCompleteStructure
}

/**
 * 生成唯一 ID
 *
 * 用于代码块等需要唯一标识的元素
 *
 * @param prefix ID 前缀
 * @returns 唯一 ID 字符串
 */
let idCounter = 0
export function generateUniqueId(prefix = 'md'): string {
  return `${prefix}-${++idCounter}-${Date.now().toString(36)}`
}

/**
 * 重置 ID 计数器
 *
 * 在每次渲染会话开始时调用，确保 ID 可预测
 */
export function resetIdCounter(): void {
  idCounter = 0
}

/**
 * 将文本编码为 Base64
 *
 * 用于在 data 属性中存储原始 HTML 内容
 *
 * @param text 原始文本
 * @returns Base64 编码后的字符串
 */
export function encodeToBase64(text: string): string {
  try {
    return btoa(unescape(encodeURIComponent(text)))
  } catch {
    return ''
  }
}

/**
 * 从 Base64 解码文本
 *
 * @param base64 Base64 编码的字符串
 * @returns 解码后的原始文本
 */
export function decodeFromBase64(base64: string): string {
  try {
    return decodeURIComponent(escape(atob(base64)))
  } catch {
    return ''
  }
}

/**
 * 生成锚点 ID
 *
 * 将标题文本转换为 URL 友好的锚点 ID
 *
 * @param text 标题文本
 * @returns 锚点 ID
 */
export function generateAnchorId(text: string): string {
  return text
    .toLowerCase()
    .replace(/<[^>]*>/g, '')
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
    .replace(/^-|-$/g, '')
}
