<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { BookOutlined, WarningOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'

interface TocItem {
  id: string
  title: string
  level: number
  children?: TocItem[]
}

const activeSection = ref('')
const tocItems = ref<TocItem[]>([])

/**
 * 生成目录
 */
const generateToc = () => {
  const headings = document.querySelectorAll('.doc-content h1, .doc-content h2, .doc-content h3')
  const toc: TocItem[] = []
  let currentH1: TocItem | null = null
  let currentH2: TocItem | null = null

  headings.forEach((heading, index) => {
    const id = `section-${index}`
    heading.id = id

    const item: TocItem = {
      id,
      title: heading.textContent || '',
      level: parseInt(heading.tagName.charAt(1)),
      children: []
    }

    if (item.level === 1) {
      currentH1 = item
      toc.push(item)
    } else if (item.level === 2) {
      if (currentH1) {
        currentH1.children = currentH1.children || []
        currentH1.children.push(item)
      }
      currentH2 = item
    } else if (item.level === 3) {
      if (currentH2 && currentH1) {
        const h2Parent = currentH1.children?.find(h2 => h2.id === currentH2?.id)
        if (h2Parent) {
          h2Parent.children = h2Parent.children || []
          h2Parent.children.push(item)
        }
      }
    }
  })

  tocItems.value = toc
}

/**
 * 滚动到指定章节
 */
const scrollToSection = (id: string) => {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeSection.value = id
  }
}

/**
 * 监听滚动高亮当前章节
 */
const handleScroll = () => {
  const headings = document.querySelectorAll('.doc-content h1, .doc-content h2, .doc-content h3')
  let currentId = ''

  headings.forEach((heading) => {
    const rect = heading.getBoundingClientRect()
    if (rect.top <= 100) {
      currentId = heading.id
    }
  })

  if (currentId) {
    activeSection.value = currentId
  }
}

onMounted(() => {
  nextTick(() => {
    generateToc()
    window.addEventListener('scroll', handleScroll)
  })
})
</script>

<template>
  <div class="doc-page">
    <aside class="doc-sidebar">
      <div class="doc-sidebar-header">
        <BookOutlined class="doc-sidebar-icon" />
        <span class="doc-sidebar-title">扩展开发指南</span>
      </div>
      <nav class="doc-toc">
        <template v-for="h1 in tocItems" :key="h1.id">
          <div
            class="doc-toc-item doc-toc-h1"
            :class="{ active: activeSection === h1.id }"
            @click="scrollToSection(h1.id)"
          >
            {{ h1.title }}
          </div>
          <template v-for="h2 in h1.children" :key="h2.id">
            <div
              v-if="h1.children && h1.children.length > 0"
              class="doc-toc-item doc-toc-h2"
              :class="{ active: activeSection === h2.id }"
              @click="scrollToSection(h2.id)"
            >
              {{ h2.title }}
            </div>
            <template v-for="h3 in h2.children" :key="h3.id">
              <div
                v-if="h2.children && h2.children.length > 0"
                class="doc-toc-item doc-toc-h3"
                :class="{ active: activeSection === h3.id }"
                @click="scrollToSection(h3.id)"
              >
                {{ h3.title }}
              </div>
            </template>
          </template>
        </template>
      </nav>
    </aside>

    <main class="doc-main">
      <article class="doc-content">
        <h1>Markdown 扩展开发指南</h1>

        <p class="doc-intro">
          本指南介绍如何使用扩展系统的两种核心机制：<strong>容器扩展自动发现</strong>和<strong>渲染器处理器覆盖</strong>。
          这两种机制让扩展开发更加灵活和便捷。
        </p>

        <!-- 机制概览 -->
        <h2>机制概览</h2>
        <div class="doc-table-wrapper">
          <table class="doc-table">
            <thead>
              <tr>
                <th>机制</th>
                <th>用途</th>
                <th>适用场景</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><strong>容器扩展自动发现</strong></td>
                <td>定义全新的 Markdown 语法</td>
                <td>自定义容器语法，如 :::tip、:::warning</td>
              </tr>
              <tr>
                <td><strong>渲染器处理器覆盖</strong></td>
                <td>覆盖现有元素的渲染方式</td>
                <td>自定义图片、代码块、链接等元素的渲染</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ========== 第一部分：容器扩展自动发现 ========== -->
        <h2>一、容器扩展自动发现</h2>

        <h3>1.1 工作原理</h3>
        <p>
          容器扩展支持自动发现机制：只需在 <code>extensions/container/</code> 目录下创建以
          <code>-extension.ts</code> 结尾的文件，并导出 <code>extensionModule</code> 对象，
          扩展就会被自动发现和注册。
        </p>

        <div class="doc-architecture">
          <div class="doc-code-block">
            <pre><code>extensions/container/
├── index.ts                    # 自动发现逻辑
├── types.ts                    # 类型定义
├── container-extension.ts      # 容器扩展 ✓ 自动发现
└── your-extension.ts           # 你的扩展 ✓ 自动发现（需以 -extension.ts 结尾）</code></pre>
          </div>
        </div>

        <h3>1.2 快速开始</h3>
        <p>创建一个新的扩展只需两步：</p>

        <div class="doc-step">
          <div class="doc-step-number">1</div>
          <div class="doc-step-content">
            <strong>创建扩展文件</strong>
            <p>在 <code>extensions/container/</code> 目录下创建 <code>xxx-extension.ts</code> 文件：</p>
          </div>
        </div>

        <div class="doc-code-block">
          <pre><code>// notice-extension.ts
import type { MarkdownExtension, CustomToken } from '../../core/types'
import type { ExtensionModule } from './types'

/**
 * 公告扩展类
 *
 * 支持语法：
 * :::notice 标题
 * 内容
 * :::
 */
class NoticeExtension implements MarkdownExtension {
  readonly name = 'notice'
  readonly level = 'block' as const
  readonly priority = 65

  start(src: string): number {
    return src.indexOf(':::notice')
  }

  tokenizer(src: string): CustomToken | undefined {
    const match = src.match(/^:::notice\s*(.*?)\n([\s\S]*?):::\s*(?:\n|$)/)
    if (match) {
      return {
        type: 'notice',
        raw: match[0],
        text: match[2]!.trim(),
        // @ts-ignore - 自定义字段
        title: match[1]!.trim()
      }
    }
    return undefined
  }

  renderer(token: CustomToken): string {
    // @ts-ignore
    const title = token.title || '公告'
    return `&lt;div class="md-notice"&gt;
      &lt;p class="md-notice-title"&gt;📢 ${title}&lt;/p&gt;
      &lt;div class="md-notice-content"&gt;${token.text ?? ''}&lt;/div&gt;
    &lt;/div&gt;`
  }
}

const noticeExtension = new NoticeExtension()

/**
 * 导出扩展模块 - 自动发现的关键！
 */
export const extensionModule: ExtensionModule&lt;NoticeExtension&gt; = {
  meta: {
    name: 'notice',
    description: '公告容器，用于显示重要通知',
    version: '1.0.0',
    author: 'your-name',
    enabled: true,      // 设为 false 可禁用此扩展
    priority: 65,       // 优先级，数字越小越先执行
  },
  extension: noticeExtension,
  // 可选：初始化函数
  setup: (engine) => {
    console.log('[NoticeExtension] 扩展已加载')
  },
}</code></pre>
        </div>

        <div class="doc-step">
          <div class="doc-step-number">2</div>
          <div class="doc-step-content">
            <strong>完成！</strong>
            <p>扩展会自动被发现和注册，无需手动导入。</p>
          </div>
        </div>

        <h3>1.3 扩展模块接口</h3>
        <div class="doc-code-block">
          <pre><code>interface ExtensionModule&lt;T extends MarkdownExtension&gt; {
  /**
   * 扩展元数据
   */
  meta: {
    name: string         // 扩展名称（唯一）
    description?: string // 扩展描述
    version?: string     // 版本号
    author?: string      // 作者
    enabled?: boolean    // 是否启用，默认 true
    priority?: number    // 优先级，默认 100
  }

  /**
   * 扩展实例
   */
  extension: T

  /**
   * 可选：初始化函数
   * 在注册到引擎前调用
   */
  setup?: (engine: unknown) => void | Promise&lt;void&gt;
}</code></pre>
        </div>

        <h3>1.4 文件命名约定</h3>
        <div class="doc-warning-list">
          <div class="doc-warning-item success">
            <CheckCircleOutlined class="doc-warning-icon success" />
            <span><strong>正确</strong>：<code>tip-extension.ts</code>、<code>notice-extension.ts</code></span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>错误</strong>：<code>tip.ts</code>、<code>notice.ts</code>（不会被自动发现）</span>
          </div>
        </div>

        <h3>1.5 高级用法：支持嵌套内容</h3>
        <p>当扩展内容包含 Markdown 时，需要设置内部渲染器：</p>

        <div class="doc-code-block">
          <pre><code>class NoticeExtension implements MarkdownExtension {
  // ... 其他代码

  private innerRender: (text: string) => string = (t) => t

  /**
   * 设置内部渲染器（用于解析嵌套的 Markdown）
   */
  setInnerRender(render: (text: string) => string): void {
    this.innerRender = render
  }

  renderer(token: CustomToken): string {
    // 使用 innerRender 解析内容中的 Markdown
    const content = this.innerRender(token.text ?? '')
    return `&lt;div class="md-notice"&gt;${content}&lt;/div&gt;`
  }
}

// 扩展模块定义
export const extensionModule: ExtensionModule&lt;NoticeExtension&gt; = {
  meta: { name: 'notice', /* ... */ },
  extension: noticeExtension,
  // setup 函数中设置内部渲染器
  setup: (engine) => {
    if (engine && typeof engine === 'object' && 'render' in engine) {
      noticeExtension.setInnerRender(
        (text) => (engine as { render: (t: string) => string }).render(text)
      )
    }
  },
}</code></pre>
        </div>

        <h3>1.6 手动使用自动加载器</h3>
        <p>如果需要更精细的控制，可以使用 <code>ExtensionAutoLoader</code>：</p>

        <div class="doc-code-block">
          <pre><code>import {
  MarkdownEngine,
  createAutoLoader,
  createRendererExtension
} from '@/utils/chat/markdown'

const engine = new MarkdownEngine()

// 注册渲染器扩展
engine.registerMarkedExtension(createRendererExtension())

// 使用自动加载器
const autoLoader = createAutoLoader(engine)

// 可选：排除某些扩展
autoLoader.exclude('example')

// 可选：只包含特定扩展
// autoLoader.only('notice', 'tip')

// 初始化并注册
autoLoader.setupAll()
autoLoader.getExtensions().forEach(ext => engine.registerExtension(ext))

engine.use()</code></pre>
        </div>

        <!-- ========== 第二部分：渲染器处理器覆盖 ========== -->
        <h2>二、渲染器处理器覆盖</h2>

        <h3>2.1 工作原理</h3>
        <p>
          渲染器扩展用于覆盖现有 Markdown 元素的渲染方式。
          通过处理器注册表，你可以轻松覆盖任何默认处理器（如 image、code、link 等）。
        </p>

        <h3>2.2 快速开始</h3>
        <div class="doc-code-block">
          <pre><code>import {
  createRendererExtension,
  type HandlerConfig
} from '@/utils/chat/markdown'
import type { Tokens } from 'marked'

// 定义自定义处理器
const customHandlers: HandlerConfig = {
  // 覆盖图片渲染
  image: (token: Tokens.Image): string => {
    return `&lt;img
      src="${token.href}"
      alt="${token.text || ''}"
      class="my-custom-image"
      loading="lazy"
    /&gt;`
  },

  // 覆盖代码块渲染
  code: (token: Tokens.Code): string => {
    return `&lt;div class="my-code-block"&gt;
      &lt;code class="language-${token.lang || ''}"&gt;${token.text}&lt;/code&gt;
    &lt;/div&gt;`
  }
}

// 创建带有自定义处理器的渲染器扩展
const myRendererExtension = createRendererExtension({
  handlers: customHandlers
})</code></pre>
        </div>

        <h3>2.3 可覆盖的处理器列表</h3>
        <div class="doc-table-wrapper">
          <table class="doc-table">
            <thead>
              <tr>
                <th>处理器名称</th>
                <th>说明</th>
                <th>Token 类型</th>
              </tr>
            </thead>
            <tbody>
              <tr><td><code>code</code></td><td>代码块</td><td>Tokens.Code</td></tr>
              <tr><td><code>image</code></td><td>图片</td><td>Tokens.Image</td></tr>
              <tr><td><code>link</code></td><td>链接</td><td>Tokens.Link</td></tr>
              <tr><td><code>heading</code></td><td>标题</td><td>Tokens.Heading</td></tr>
              <tr><td><code>table</code></td><td>表格</td><td>Tokens.Table</td></tr>
              <tr><td><code>list</code></td><td>列表</td><td>Tokens.List</td></tr>
              <tr><td><code>listitem</code></td><td>列表项</td><td>Tokens.ListItem</td></tr>
            </tbody>
          </table>
        </div>

        <h3>2.4 完整示例：自定义图片渲染器</h3>
        <div class="doc-code-block">
          <pre><code>import {
  createRendererExtension,
  MarkdownEngine,
  escapeHtml
} from '@/utils/chat/markdown'
import type { Tokens } from 'marked'

/**
 * 自定义图片处理器
 *
 * 功能：
 * - 添加点击预览
 * - 支持 figcaption
 * - 添加 loading="lazy"
 */
function customImageHandler(token: Tokens.Image): string {
  const { href, title, text } = token
  const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
  const altAttr = text ? ` alt="${escapeHtml(text)}"` : ''

  return `
    &lt;figure class="my-figure"&gt;
      &lt;img
        src="${href}"${altAttr}${titleAttr}
        loading="lazy"
        class="my-image"
        onclick="openImagePreview(this)"
      /&gt;
      ${text ? `&lt;figcaption class="my-figcaption"&gt;${escapeHtml(text)}&lt;/figcaption&gt;` : ''}
    &lt;/figure&gt;
  `
}

// 创建渲染器扩展
const myRenderer = createRendererExtension({
  handlers: {
    image: customImageHandler
  }
})

// 注册到引擎
const engine = new MarkdownEngine()
engine.registerMarkedExtension(myRenderer).use()</code></pre>
        </div>

        <h3>2.5 全局覆盖处理器</h3>
        <p>如果希望全局覆盖某个处理器，可以直接操作全局注册表：</p>

        <div class="doc-code-block">
          <pre><code>import { globalHandlerRegistry } from '@/utils/chat/markdown'
import type { Tokens } from 'marked'

// 全局覆盖图片处理器
globalHandlerRegistry.register('image', (token: Tokens.Image) => {
  return `&lt;img src="${token.href}" class="global-custom-image" /&gt;`
}, {
  isDefault: false,
  description: '全局自定义图片处理器'
})</code></pre>
        </div>

        <h3>2.6 处理器函数签名</h3>
        <div class="doc-code-block">
          <pre><code>interface HandlerFunctions {
  // 简单处理器：不需要 parser
  image: (token: Tokens.Image) => string
  hr: () => string
  br: () => string

  // 复杂处理器：需要 parser 解析子元素
  code: (token: Tokens.Code, parser?: unknown) => string
  link: (token: Tokens.Link, parser?: unknown) => string
  heading: (token: Tokens.Heading, parser?: unknown) => string
  table: (token: Tokens.Table, parser?: unknown) => string
  list: (token: Tokens.List, parser?: unknown) => string
  listitem: (token: Tokens.ListItem, parser?: unknown) => string
}</code></pre>
        </div>

        <h3>2.7 使用 Parser 解析子元素</h3>
        <p>某些处理器需要解析嵌套的 Markdown 元素：</p>

        <div class="doc-code-block">
          <pre><code>import type { Tokens } from 'marked'

function customLinkHandler(
  token: Tokens.Link,
  parser?: { parseInline: (tokens: unknown[]) => string }
): string {
  // 使用 parser 解析链接文本中的行内元素
  const text = parser
    ? parser.parseInline(token.tokens || [])
    : token.text

  const isExternal = token.href.startsWith('http')

  return `&lt;a
    href="${token.href}"
    class="my-link ${isExternal ? 'external' : ''}"
    ${isExternal ? 'target="_blank" rel="noopener"' : ''}
  &gt;${text}&lt;/a&gt;`
}</code></pre>
        </div>

        <!-- ========== 最佳实践 ========== -->
        <h2>三、最佳实践</h2>

        <h3>3.1 两种机制配合使用</h3>
        <div class="doc-code-block">
          <pre><code>import {
  MarkdownEngine,
  createRendererExtension,
  createAutoLoader
} from '@/utils/chat/markdown'

const engine = new MarkdownEngine()

// 1. 注册渲染器扩展（覆盖默认元素渲染）
engine.registerMarkedExtension(createRendererExtension({
  handlers: {
    image: myCustomImageHandler
  }
}))

// 2. 注册自动发现的容器扩展
const autoLoader = createAutoLoader(engine)
autoLoader.setupAll()
autoLoader.getExtensions().forEach(ext => engine.registerExtension(ext))

// 3. 刷新引擎
engine.use()</code></pre>
        </div>

        <h3>3.2 注意事项</h3>
        <div class="doc-warning-list">
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>文件命名</strong>：容器扩展文件必须以 <code>-extension.ts</code> 结尾才会被自动发现</span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>扩展名称唯一</strong>：<code>meta.name</code> 必须唯一，重复会报错</span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>调用 use()</strong>：注册扩展后必须调用 <code>engine.use()</code> 使扩展生效</span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>XSS 安全</strong>：引擎默认启用 DOMPurify，自定义标签可能被过滤</span>
          </div>
        </div>

        <h3>3.3 扩展目录结构参考</h3>
        <div class="doc-code-block">
          <pre><code>markdown/
├── core/                          # 核心引擎
│   ├── engine.ts
│   └── types.ts
├── extensions/
│   ├── container/                 # 容器扩展（自动发现）
│   │   ├── index.ts               # 自动发现逻辑
│   │   ├── types.ts               # 类型定义
│   │   ├── container-extension.ts # 默认容器
│   │   ├── notice-extension.ts    # 公告容器（示例）
│   │   └── your-extension.ts      # 你的扩展
│   ├── renderer/                  # 渲染器扩展
│   │   ├── renderer-extension.ts  # 渲染器扩展
│   │   └── handlers/              # 处理器
│   │       ├── registry.ts        # 处理器注册表
│   │       ├── image-handler.ts
│   │       └── ...
│   └── katex/                     # KaTeX 扩展
├── utils/                         # 工具函数
└── index.ts                       # 主入口</code></pre>
        </div>

        <!-- ========== 快速参考 ========== -->
        <h2>四、快速参考</h2>

        <h3>创建容器扩展</h3>
        <div class="doc-code-block">
          <pre><code>// 1. 创建 xxx-extension.ts 文件
// 2. 实现 MarkdownExtension 接口
// 3. 导出 extensionModule 对象
// 4. 完成！自动发现和注册</code></pre>
        </div>

        <h3>覆盖渲染器处理器</h3>
        <div class="doc-code-block">
          <pre><code>import { createRendererExtension } from '@/utils/chat/markdown'

const ext = createRendererExtension({
  handlers: {
    image: (token) => `&lt;img src="${token.href}" /&gt;`,
    code: (token) => `&lt;pre&gt;${token.text}&lt;/pre&gt;`
  }
})</code></pre>
        </div>

        <h3>使用默认引擎</h3>
        <div class="doc-code-block">
          <pre><code>import { renderMarkdown } from '@/utils/chat/markdown'

// 使用预配置的默认引擎
const html = renderMarkdown('# Hello World')</code></pre>
        </div>
      </article>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/doc/index.scss' as *;

.doc-intro {
  font-size: 1.1rem;
  color: var(--text-color-secondary);
  margin-bottom: 2rem;
  line-height: 1.8;
}

.doc-architecture {
  margin: 1.5rem 0;
  padding: 1rem;
  background: var(--bg-color-secondary);
  border-radius: 8px;
}

.doc-step {
  display: flex;
  gap: 1rem;
  margin: 1.5rem 0;
  padding: 1rem;
  background: var(--bg-color-secondary);
  border-radius: 8px;
  align-items: flex-start;

  &-number {
    width: 32px;
    height: 32px;
    background: var(--primary-color);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    flex-shrink: 0;
  }

  &-content {
    flex: 1;

    strong {
      display: block;
      margin-bottom: 0.5rem;
    }

    p {
      margin: 0;
      color: var(--text-color-secondary);
    }
  }
}

.doc-warning-item {
  &.success {
    background: rgba(82, 196, 26, 0.1);
    border-left: 4px solid #52c41a;
  }

  .doc-warning-icon.success {
    color: #52c41a;
  }
}
</style>
