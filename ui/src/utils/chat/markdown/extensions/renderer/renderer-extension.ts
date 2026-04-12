/**
 * 描述：自定义渲染器扩展
 *
 * 整合所有渲染处理器，提供统一的 Marked 渲染器扩展
 *
 * @author huxuehao
 **/

import type { MarkedExtension, Tokens } from 'marked'
import {
  codeHandler,
  linkHandler,
  imageHandler,
  tableHandler,
  headingHandler,
  listitemHandler,
} from './handlers'

/**
 * 创建自定义渲染器扩展
 *
 * @returns Marked 扩展配置
 */
export function createRendererExtension(): MarkedExtension {
  return {
    renderer: {
      // 代码块：高亮、复制按钮、HTML 预览
      code(token: Tokens.Code) {
        return codeHandler(token)
      },

      // 行内代码
      codespan(token: Tokens.Codespan) {
        return `<code class="md-inline-code">${token.text}</code>`
      },

      // 链接：外链新窗口打开
      link(token: Tokens.Link) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return linkHandler(token, (this as any).parser)
      },

      // 图片：懒加载、预览
      image(token: Tokens.Image) {
        return imageHandler(token)
      },

      // 表格：可滚动容器
      table(token: Tokens.Table) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return tableHandler(token, (this as any).parser)
      },

      // 标题：锚点
      heading(token: Tokens.Heading) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return headingHandler(token, (this as any).parser)
      },

      // 列表项：任务列表支持
      listitem(token: Tokens.ListItem) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return listitemHandler(token, (this as any).parser)
      },

      // 加粗文本
      strong(token: Tokens.Strong) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return `<strong>${(this as any).parser.parseInline(token.tokens)}</strong>`
      },

      // 斜体文本
      em(token: Tokens.Em) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return `<em>${(this as any).parser.parseInline(token.tokens)}</em>`
      },

      // 引用块
      blockquote(token: Tokens.Blockquote) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return `<blockquote class="md-blockquote">${(this as any).parser.parse(token.tokens)}</blockquote>`
      },

      // 分割线
      hr() {
        return '<hr class="md-hr" />'
      },

      // 段落
      paragraph(token: Tokens.Paragraph) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return `<p>${(this as any).parser.parseInline(token.tokens)}</p>`
      },

      // 换行
      br() {
        return '<br />'
      },
    },
  }
}

/**
 * 默认渲染器扩展实例
 */
export const rendererExtension = createRendererExtension()
