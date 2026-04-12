/**
 * 描述：自定义渲染器扩展
 *
 * 整合所有渲染处理器，提供统一的 Marked 渲染器扩展
 * 支持处理器覆盖机制
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
  listHandler,
  listitemHandler,
  RendererHandlerRegistry,
  globalHandlerRegistry,
  type HandlerConfig,
  type HandlerFunctions,
  type HandlerName,
} from './handlers'

/**
 * 初始化标志
 */
let initialized = false

/**
 * 确保默认处理器已初始化
 */
function ensureInitialized(): void {
  if (initialized) return
  initialized = true
  
  globalHandlerRegistry
    .register('code', codeHandler as HandlerFunctions['code'], { isDefault: true, description: '代码块渲染' })
    .register('link', linkHandler as HandlerFunctions['link'], { isDefault: true, description: '链接渲染' })
    .register('image', imageHandler as HandlerFunctions['image'], { isDefault: true, description: '图片渲染' })
    .register('table', tableHandler as HandlerFunctions['table'], { isDefault: true, description: '表格渲染' })
    .register('heading', headingHandler as HandlerFunctions['heading'], { isDefault: true, description: '标题渲染' })
    .register('list', listHandler as HandlerFunctions['list'], { isDefault: true, description: '列表渲染' })
    .register('listitem', listitemHandler as HandlerFunctions['listitem'], { isDefault: true, description: '列表项渲染' })
}

/**
 * 渲染器扩展配置
 */
export interface RendererExtensionConfig {
  /**
   * 自定义处理器配置
   *
   * 用于覆盖默认处理器
   */
  handlers?: HandlerConfig
}

/**
 * 创建自定义渲染器扩展
 *
 * @param config 扩展配置
 * @returns Marked 扩展配置
 *
 * @example
 * ```typescript
 * // 使用默认处理器
 * const ext = createRendererExtension()
 *
 * // 覆盖特定处理器
 * const ext = createRendererExtension({
 *   handlers: {
 *     image: (token) => `<img src="${token.href}" />`
 *   }
 * })
 * ```
 */
export function createRendererExtension(config: RendererExtensionConfig = {}): MarkedExtension {
  // 确保默认处理器已初始化
  ensureInitialized()
  
  // 克隆全局注册表，避免修改全局状态
  const registry = globalHandlerRegistry.clone()

  // 应用自定义处理器覆盖
  if (config.handlers) {
    registry.registerAll(config.handlers, { isDefault: false })
  }

  // 创建渲染器扩展
  return buildRendererExtension(registry)
}

/**
 * 根据注册表构建渲染器扩展
 */
function buildRendererExtension(registry: RendererHandlerRegistry): MarkedExtension {
  return {
    renderer: {
      // 代码块：高亮、复制按钮、HTML 预览
      code(token: Tokens.Code) {
        const handler = registry.get('code')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
      },

      // 行内代码
      codespan(token: Tokens.Codespan) {
        return `<code class="md-inline-code">${token.text}</code>`
      },

      // 链接：外链新窗口打开
      link(token: Tokens.Link) {
        const handler = registry.get('link')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
      },

      // 图片：懒加载、预览
      image(token: Tokens.Image) {
        const handler = registry.get('image')
        return handler ? handler(token) : ''
      },

      // 表格：可滚动容器
      table(token: Tokens.Table) {
        const handler = registry.get('table')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
      },

      // 标题：锚点
      heading(token: Tokens.Heading) {
        const handler = registry.get('heading')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
      },

      // 列表：支持有序、无序、任务列表
      list(token: Tokens.List) {
        const handler = registry.get('list')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
      },

      // 列表项：任务列表支持
      listitem(token: Tokens.ListItem) {
        const handler = registry.get('listitem')
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return handler ? handler(token, (this as any).parser) : ''
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

// 重新导出类型和注册表
export {
  RendererHandlerRegistry,
  globalHandlerRegistry,
  type HandlerConfig,
  type HandlerFunctions,
  type HandlerName,
}
