/**
 * 描述：表格渲染处理器
 *
 * 提供增强的表格渲染功能：
 * - 包裹可滚动容器
 * - 支持对齐方式
 *
 * @author huxuehao
 **/

import type { Tokens } from 'marked'

/**
 * 表格渲染处理器
 *
 * @param token 表格 Token
 * @param parser 解析器对象
 * @returns 渲染后的 HTML
 */
export function tableHandler(
  token: Tokens.Table,
  parser: { parseInline: (tokens: Tokens.Generic[]) => string }
): string {
  const { header, rows } = token

  // 渲染表头
  const headerHtml = header
    .map((cell: Tokens.TableCell) => {
      const align = cell.align ? ` align="${cell.align}"` : ''
      return `<th${align}>${parser.parseInline(cell.tokens)}</th>`
    })
    .join('')

  // 渲染表体
  const bodyHtml = rows
    .map((row: Tokens.TableCell[]) => {
      const cells = row
        .map((cell: Tokens.TableCell) => {
          const align = cell.align ? ` align="${cell.align}"` : ''
          return `<td${align}>${parser.parseInline(cell.tokens)}</td>`
        })
        .join('')
      return `<tr>${cells}</tr>`
    })
    .join('')

  return `<div class="md-table-wrapper">
    <table class="md-table">
      <thead><tr>${headerHtml}</tr></thead>
      <tbody>${bodyHtml}</tbody>
    </table>
  </div>`
}
