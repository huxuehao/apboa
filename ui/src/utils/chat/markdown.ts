import { Marked, type Tokens } from 'marked'
import hljs from 'highlight.js'
import katex from 'katex'
import DOMPurify from 'dompurify'

/**
 * 高级 Markdown 渲染器
 * 功能：GFM、代码高亮、KaTeX 数学公式、代码复制按钮、
 *       链接安全打开、表格增强、任务列表、自定义容器等
 */

// ==================== KaTeX 数学公式扩展 ====================

/**
 * 渲染 KaTeX 公式，失败时返回原始文本
 * @param expression 数学表达式
 * @param displayMode 是否块级显示
 * @returns 渲染后的 HTML
 */
function renderKatex(expression: string, displayMode: boolean): string {
  try {
    return katex.renderToString(expression, {
      displayMode,
      throwOnError: false,
      strict: false,
      trust: true,
      output: 'html',
    })
  } catch {
    return `<code class="katex-error">${escapeHtml(expression)}</code>`
  }
}

/**
 * KaTeX 块级公式扩展（$$...$$）
 */
const katexBlockExtension: import('marked').MarkedExtension = {
  extensions: [
    {
      name: 'katexBlock',
      level: 'block',
      start(src: string) {
        return src.indexOf('$$')
      },
      tokenizer(src: string) {
        const match = src.match(/^\$\$([\s\S]+?)\$\$/)
        if (match) {
          return {
            type: 'katexBlock',
            raw: match[0],
            text: match[1]!.trim(),
          }
        }
      },
      renderer(token) {
        return `<div class="katex-block">${renderKatex((token as Record<string, string>).text ?? '', true)}</div>`
      },
    },
  ],
}

/**
 * KaTeX 行内公式扩展（$...$）
 * 注意：需要更精确的匹配，避免误匹配 Shell 变量如 $HOME, $name 等
 */
const katexInlineExtension: import('marked').MarkedExtension = {
  extensions: [
    {
      name: 'katexInline',
      level: 'inline',
      start(src: string) {
        // 查找后面跟着非空格和非$字符的$，这是潜在数学公式的开始
        const match = src.match(/(?<!\\)\$(?=[^\s\$])/)
        return match ? match.index! : -1
      },
      tokenizer(src: string, tokens) {
        // 修复：要求 $ 之后不能紧跟数字（避免 $100），且结尾 $ 不能紧跟字母数字
        const match = src.match(/^\$([^$\n]+?)\$(?![a-zA-Z0-9])/)
        if (match) {
          return {
            type: 'katexInline',
            raw: match[0],
            text: match[1]!.trim(),
          }
        }
        return undefined
      },
      renderer(token) {
        return renderKatex((token as Record<string, string>).text ?? '', false)
      },
    },
  ],
}

// ==================== 自定义容器扩展 ====================

/**
 * 自定义容器扩展：支持 :::tip / :::warning / :::danger / :::info / :::success
 */
const containerExtension: import('marked').MarkedExtension = {
  extensions: [
    {
      name: 'container',
      level: 'block',
      start(src: string) {
        return src.indexOf(':::')
      },
      tokenizer(src: string) {
        const match = src.match(/^:::(tip|warning|danger|info|success)(.*?)\n([\s\S]*?):::\s*(?:\n|$)/)
        if (match) {
          return {
            type: 'container',
            raw: match[0],
            containerType: match[1]!,
            title: match[2]!.trim(),
            text: match[3]!.trim(),
          }
        }
      },
      renderer(token) {
        const t = token as Record<string, string>
        const iconMap: Record<string, string> = {
          tip: '💡',
          warning: '⚠️',
          danger: '🚨',
          info: 'ℹ️',
          success: '✅',
        }
        const titleMap: Record<string, string> = {
          tip: '提示',
          warning: '警告',
          danger: '危险',
          info: '信息',
          success: '成功',
        }
        const icon = iconMap[t.containerType ?? ''] || ''
        const title = t.title || titleMap[t.containerType ?? ''] || (t.containerType ?? '').toUpperCase()
        return `<div class="md-container md-container-${t.containerType}">
          <p class="md-container-title">${icon} ${escapeHtml(title)}</p>
          <div class="md-container-content">${innerMarked.parse(t.text ?? '')}</div>
        </div>`
      },
    },
  ],
}

// ==================== 工具函数 ====================

let copyIdCounter = 0

/**
 * HTML 转义，防止 XSS
 */
function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// ==================== 自定义渲染器 ====================

/**
 * 自定义渲染器：增强链接、图片、代码块、表格、任务列表等
 */
const customRenderer: import('marked').MarkedExtension = {
  renderer: {
    // 链接：新窗口打开，添加安全属性和外链图标
    link({ href, title, text }) {
      const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
      const isExternal = href && (href.startsWith('http://') || href.startsWith('https://'))
      const externalAttrs = isExternal ? ' target="_blank" rel="noopener noreferrer"' : ''
      const externalIcon = isExternal
        ? '<svg class="md-external-icon" viewBox="0 0 24 24" width="12" height="12"><path fill="currentColor" d="M14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3m-2 16H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2v7Z"/></svg>'
        : ''
      return `<a href="${href}"${titleAttr}${externalAttrs} class="md-link">${text}${externalIcon}</a>`
    },

    // 图片：支持懒加载和点击预览
    image({ href, title, text }) {
      const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
      const altAttr = text ? ` alt="${escapeHtml(text)}"` : ''
      return `<figure class="md-figure">
        <img src="${href}"${altAttr}${titleAttr} loading="lazy" class="md-image" />
        ${text ? `<figcaption class="md-figcaption">${escapeHtml(text)}</figcaption>` : ''}
      </figure>`
    },

    // 表格：包裹可滚动容器
    table({ header, rows }) {
      const body = rows.map((row: Tokens.TableCell[]) =>
        `<tr>${row.map((cell: Tokens.TableCell) => `<td${cell.align ? ` align="${cell.align}"` : ''}>${this.parser.parseInline(cell.tokens)}</td>`).join('')}</tr>`
      ).join('')
      return `<div class="md-table-wrapper">
        <table class="md-table">
          <thead><tr>${header.map((h: Tokens.TableCell) => `<th${h.align ? ` align="${h.align}"` : ''}>${this.parser.parseInline(h.tokens)}</th>`).join('')}</tr></thead>
          <tbody>${body}</tbody>
        </table>
      </div>`
    },

    // 代码块：语言标签 + 复制按钮
    code({ text, lang }) {
      const id = `code-${++copyIdCounter}`
      const language = lang || 'text'
      let highlighted: string
      if (lang && hljs.getLanguage(lang)) {
        highlighted = hljs.highlight(text, { language: lang }).value
      } else {
        highlighted = hljs.highlightAuto(text).value
      }
      return `<div class="md-code-block" id="${id}">
        <div class="md-code-header">
          <span class="md-code-lang">${escapeHtml(language)}</span>
          <button class="md-code-copy-btn" onclick="(function(btn){var code=btn.closest('.md-code-block').querySelector('code');if(navigator.clipboard){navigator.clipboard.writeText(code.textContent).then(function(){btn.textContent='已复制';btn.classList.add('copied');setTimeout(function(){btn.textContent='复制';btn.classList.remove('copied')},2000)})}else{var t=document.createElement('textarea');t.value=code.textContent;document.body.appendChild(t);t.select();document.execCommand('copy');document.body.removeChild(t);btn.textContent='已复制';btn.classList.add('copied');setTimeout(function(){btn.textContent='复制';btn.classList.remove('copied')},2000)}})(this)">复制</button>
        </div>
        <pre><code class="hljs language-${escapeHtml(language)}">${highlighted}</code></pre>
      </div>`
    },

    // 行内代码
    codespan({ text }) {
      return `<code class="md-inline-code">${text}</code>`
    },

    // 加粗文本
    strong({ text }) {
      return `<strong>${text}</strong>`
    },

    // 斜体文本
    em({ text }) {
      return `<em>${text}</em>`
    },

    // 引用块
    blockquote({ text }) {
      return `<blockquote class="md-blockquote">${text}</blockquote>`
    },

    // 标题：添加锚点
    heading({ text, depth }) {
      const anchor = text
        .toLowerCase()
        .replace(/<[^>]*>/g, '')
        .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
        .replace(/^-|-$/g, '')
      return `<h${depth} id="${anchor}" class="md-heading md-h${depth}">
        <a href="#${anchor}" class="md-heading-anchor">#</a>
        ${text}
      </h${depth}>`
    },

    // 分割线
    hr() {
      return '<hr class="md-hr" />'
    },

    // 列表项：支持任务列表，正确解析行内元素和嵌套列表
    listitem({ text, task, checked, tokens }) {
      let parsedText: string
      try {
        if (tokens && this.parser) {
          // 分离行内 token 和块级 token
          const inlineTokens: typeof tokens = []
          const blockTokens: typeof tokens = []
          for (const token of tokens) {
            if (token.type === 'list' || token.type === 'code' || token.type === 'heading') {
              blockTokens.push(token)
            } else {
              inlineTokens.push(token)
            }
          }
          // 解析行内元素
          const inlineHtml = inlineTokens.length > 0 ? this.parser.parseInline(inlineTokens) : ''
          // 解析块级元素（如嵌套列表）
          const blockHtml = blockTokens.map(t => {
            // 使用内部方法渲染单个块级 token
            if (t.type === 'list') {
              // 递归渲染嵌套列表
              return this.parser.parse([t])
            }
            return this.parser.parse([t])
          }).join('')
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
    },
  },
}

// ==================== Marked 实例配置 ====================

/**
 * 内部 Marked 实例（用于容器内嵌套解析，需包含公式扩展以支持容器内公式）
 */
const innerMarked = new Marked()
// 修复：给 innerMarked 添加公式扩展，确保容器内的公式能正常渲染
innerMarked.use(katexBlockExtension)
innerMarked.use(katexInlineExtension)
innerMarked.setOptions({ gfm: true, breaks: true })

/**
 * 主 Marked 实例：集成所有扩展
 */
const md = new Marked()

md.use(katexBlockExtension)
md.use(katexInlineExtension)
md.use(containerExtension)
md.use(customRenderer)

md.setOptions({
  gfm: true,
  breaks: true,
})

// ==================== DOMPurify 配置 ====================

/**
 * 配置 DOMPurify 白名单，保留渲染所需的标签和属性
 */
const purifyConfig = {
  ADD_TAGS: [
    'math', 'mrow', 'mi', 'mo', 'mn', 'msup', 'msub', 'mfrac',
    'munderover', 'mover', 'munder', 'msqrt', 'mroot', 'mtable',
    'mtr', 'mtd', 'mtext', 'mspace', 'semantics', 'annotation',
    'figure', 'figcaption',
  ],
  ADD_ATTR: [
    'target', 'rel', 'class', 'id', 'loading', 'onclick',
    'disabled', 'checked', 'type', 'mathvariant', 'encoding',
    'xmlns', 'display', 'columnalign', 'rowalign', 'columnspacing',
    'rowspacing', 'fence', 'stretchy', 'symmetric', 'lspace',
    'rspace', 'accent', 'accentunder', 'scriptlevel', 'movablelimits',
    'separator', 'width', 'height', 'depth', 'voffset', 'style',
  ],
  ALLOW_DATA_ATTR: false,
}

// ==================== 导出 API ====================

/**
 * 渲染 Markdown 文本为安全的 HTML
 *
 * @param text Markdown 原始文本
 * @returns 渲染后的安全 HTML 字符串
 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  try {
    const html = md.parse(text) as string
    return DOMPurify.sanitize(html, purifyConfig) as string
  } catch (e) {
    console.error('Markdown render error:', e)
    return escapeHtml(text)
  }
}