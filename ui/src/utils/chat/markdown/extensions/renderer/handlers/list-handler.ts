/**
 * 描述：列表和列表项渲染处理器
 *
 * 提供增强的列表渲染功能：
 * - 支持有序和无序列表
 * - 支持任务列表（checkbox）
 * - 正确解析行内元素和嵌套列表
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 列表渲染处理器
 *
 * @param token 列表 Token
 * @param parser 解析器对象
 * @returns 渲染后的 HTML
 */
export function listHandler(
  token: Tokens.List,
  parser: { parseInline: (tokens: Tokens.Generic[]) => string; parse: (tokens: Tokens.Generic[]) => string }
): string {
  const { ordered, start, loose, items } = token

  // 渲染列表项
  const itemsHtml = items
    .map((item) => listitemHandler(item, parser))
    .join('')

  // 根据是否有 start 属性决定是否添加 start 属性
  const startAttr = ordered && start !== 1 && start !== '' ? ` start="${start}"` : ''
  // 根据 loose 决定是否添加 loose 类
  const looseClass = loose ? 'md-list-loose' : ''

  if (ordered) {
    return `<ol class="md-list md-ol-list ${looseClass}"${startAttr}>${itemsHtml}</ol>`
  }
  return `<ul class="md-list md-ul-list ${looseClass}">${itemsHtml}</ul>`
}

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
  const { task, checked, tokens } = token

  let parsedText: string

  try {
    if (tokens && parser) {
      // 分离行内 token 和块级 token
      // 块级元素类型列表：这些元素需要使用 parse() 而不是 parseInline()
      const blockTypes = new Set([
        'list',       // 列表
        'code',       // 缩进代码块
        'fence',      // 围栏代码块
        'heading',    // 标题
        'paragraph',  // 段落
        'blockquote', // 引用块
        'table',      // 表格
        'hr',         // 分割线
        'html',       // HTML 块
      ])

      const inlineTokens: typeof tokens = []
      const blockTokens: typeof tokens = []

      for (const t of tokens) {
        if (blockTypes.has(t.type)) {
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
      parsedText = token.text ?? ''
    }
  } catch {
    parsedText = token.text ?? ''
  }

  if (task) {
    const checkedClass = checked ? 'md-task-checked' : ''
    return `<li class="md-task-item ${checkedClass}">
      <div class="md-task-content">${parsedText}</div>
    </li>`
  }

  return `<li class="md-list-item">${parsedText}</li>`
}
