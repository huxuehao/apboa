/**
 * 描述：列表项渲染处理器
 *
 * 提供增强的列表项渲染功能：
 * - 支持任务列表（checkbox）
 * - 正确解析行内元素和嵌套列表
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 列表项渲染处理器
 *
 * @param token 列表项 Token
 * @param parser 解析器对象
 * @returns 渲染后的 HTML
 */
export function listitemHandler(
  token: Tokens.ListItem,
  parser: { parseInline: (tokens: Tokens.Generic[]) => string; parse: (tokens: Tokens.Generic[]) => string }
): string {
  const { text, task, checked, tokens } = token

  let parsedText: string

  try {
    if (tokens && parser) {
      // 分离行内 token 和块级 token
      const inlineTokens: typeof tokens = []
      const blockTokens: typeof tokens = []

      for (const t of tokens) {
        if (t.type === 'list' || t.type === 'code' || t.type === 'heading') {
          blockTokens.push(t)
        } else {
          inlineTokens.push(t)
        }
      }

      // 解析行内元素
      const inlineHtml = inlineTokens.length > 0 ? parser.parseInline(inlineTokens) : ''

      // 解析块级元素（如嵌套列表）
      const blockHtml = blockTokens
        .map((t) => {
          return parser.parse([t])
        })
        .join('')

      parsedText = inlineHtml + blockHtml
    } else {
      parsedText = text ?? ''
    }
  } catch {
    parsedText = text ?? ''
  }

  if (task) {
    const checkedClass = checked ? 'md-task-checked' : ''
    const checkedAttr = checked ? 'checked disabled' : 'disabled'
    return `<li class="md-task-item ${checkedClass}">
      <input type="checkbox" ${checkedAttr} class="md-task-checkbox" />${parsedText}
    </li>`
  }

  return `<li class="md-list-item">${parsedText}</li>`
}
