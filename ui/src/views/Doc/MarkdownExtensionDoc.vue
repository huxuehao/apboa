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
<!--        <BookOutlined class="doc-sidebar-icon" />-->
        <img src="@/assets/images/logo/logo.png" alt="logo" class="doc-sidebar-icon" />
        <span class="doc-sidebar-title">Markdown 扩展开发指南</span>
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
          容器扩展支持自动发现机制：只需在 <code>utils/chat/markdown/extensions/container/</code> 目录下创建以
          <code>-extension.ts</code> 结尾的文件，并导出 <code>extensionModule</code> 对象，
          扩展就会被自动发现和注册。
        </p>

        <div class="doc-architecture">
          <div class="doc-code-block">
            <pre><code>utils/chat/markdown/extensions/container/
├── index.ts                    # 自动发现逻辑
├── types.ts                    # 类型定义
├── info-extension.ts      # 容器扩展 ✓ 自动发现
└── your-extension.ts           # 你的扩展 ✓ 自动发现（需以 -extension.ts 结尾）</code></pre>
          </div>
        </div>

        <h3>1.2 快速开始</h3>
        <p>创建一个新的扩展只需两步：</p>

        <div class="doc-step">
          <div class="doc-step-number">1</div>
          <div class="doc-step-content">
            <strong>创建扩展文件</strong>
            <p>在 <code>utils/chat/markdown/extensions/container/</code> 目录下创建 <code>xxx-extension.ts</code> 文件：</p>
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

        <h3>1.3 扩展模块接口详解</h3>
        <p><code>extensionModule</code> 是自动发现机制的核心，它告诉系统你的扩展长什么样、怎么用：</p>

        <div class="doc-code-block">
          <pre><code>// 你必须导出这个对象
export const extensionModule: ExtensionModule&lt;MyExtension&gt; = {
  // ========== 元数据：描述你的扩展 ==========
  meta: {
    name: 'notice',           // 【必填】扩展名称，必须唯一
    description: '公告容器',   // 【可选】描述，方便别人理解
    version: '1.0.0',         // 【可选】版本号
    author: 'your-name',      // 【可选】作者
    enabled: true,            // 【可选】是否启用，默认 true。设为 false 可临时禁用
    priority: 65,             // 【可选】优先级，数字越小越先执行，默认 100
  },

  // ========== 扩展实例：你的扩展逻辑 ==========
  extension: myExtension,     // 你的扩展类实例

  // ========== 初始化函数：扩展加载时执行 ==========
  setup: (engine) => {
    // 可选：做一些初始化工作
    // 比如设置嵌套渲染器、打印日志等
    console.log('扩展已加载')
  },
}</code></pre>
        </div>

        <div class="doc-tip">
          <strong>关键点：</strong>
          <ul>
            <li><code>meta.name</code> 必须唯一，重复会报错</li>
            <li><code>meta.enabled = false</code> 可以临时禁用扩展，无需删除文件</li>
            <li><code>meta.priority</code> 影响解析顺序，数字越小越先执行</li>
          </ul>
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

        <h3>1.5 高级用法：支持嵌套 Markdown</h3>

        <div class="doc-scenario">
          <strong>场景说明</strong>
          <p>假设你的容器内容包含 Markdown 语法：</p>
          <pre><code>:::notice 重要通知
这里是 **加粗文字** 和 [链接](http://example.com)
:::</code></pre>
          <p>如果不处理嵌套，这些 <code>**加粗**</code> 和 <code>[链接]()</code> 不会被解析，会原样输出。</p>
        </div>

        <p>解决方法：在 <code>setup</code> 函数中设置内部渲染器：</p>

        <div class="doc-code-block">
          <pre><code>class NoticeExtension implements MarkdownExtension {
  // 1. 定义内部渲染器
  private innerRender: (text: string) => string = (t) => t

  // 2. 提供设置方法
  setInnerRender(render: (text: string) => string): void {
    this.innerRender = render
  }

  // 3. 在渲染时使用内部渲染器
  renderer(token: CustomToken): string {
    // 用 innerRender 解析内容中的 Markdown
    const content = this.innerRender(token.text ?? '')
    return `&lt;div class="md-notice"&gt;${content}&lt;/div&gt;`
  }
}

const noticeExtension = new NoticeExtension()

// 4. 在 setup 中设置内部渲染器
export const extensionModule: ExtensionModule&lt;NoticeExtension&gt; = {
  meta: { name: 'notice', enabled: true, priority: 65 },
  extension: noticeExtension,
  setup: (engine) => {
    // engine 就是 MarkdownEngine 实例，有 render 方法
    if (engine && typeof engine === 'object' && 'render' in engine) {
      noticeExtension.setInnerRender(
        (text) => (engine as { render: (t: string) => string }).render(text)
      )
    }
  },
}</code></pre>
        </div>

        <div class="doc-tip">
          <strong>简单理解：</strong>把容器内容交给 Markdown 引擎再渲染一遍。
        </div>

        <h3>1.6 进阶：手动控制扩展加载</h3>

        <div class="doc-scenario">
          <strong>什么时候需要手动控制？</strong>
          <ul>
            <li>你想排除某些扩展不加载</li>
            <li>你想只加载特定扩展</li>
            <li>你不用默认引擎，而是创建自己的引擎实例</li>
          </ul>
        </div>

        <p>使用 <code>createAutoLoader</code> 手动控制：</p>

        <div class="doc-code-block">
          <pre><code>import {
  MarkdownEngine,
  createAutoLoader,
  createRendererExtension
} from '@/utils/chat/markdown'

// 1. 创建自己的引擎实例
const engine = new MarkdownEngine()

// 2. 创建自动加载器
const autoLoader = createAutoLoader(engine)

// 3. 排除不想加载的扩展（可选）
autoLoader.exclude('example', 'test')

// 或者只加载特定扩展（可选）
// autoLoader.only('notice', 'tip')

// 4. 初始化所有扩展
autoLoader.setupAll()

// 5. 把扩展注册到引擎
autoLoader.getExtensions().forEach(ext => {
  engine.registerExtension(ext)
})

// 6. 刷新引擎
engine.use()</code></pre>
        </div>

        <div class="doc-tip">
          <strong>大多数情况下你不需要这个！</strong>
          默认引擎已经自动加载所有扩展。只有特殊需求才需要手动控制。
        </div>

        <!-- ========== 第二部分：渲染器处理器覆盖 ========== -->
        <h2>二、渲染器处理器覆盖</h2>

        <div class="doc-scenario">
          <strong>这是干嘛的？</strong>
          <p>
            Markdown 渲染器会把 <code>![图片](url)</code> 变成 <code>&lt;img src="url"&gt;</code>。<br>
            如果你想让图片变成别的样子（比如加 lazy loading、加点击预览），就需要覆盖 image 处理器。
          </p>
        </div>

        <h3>2.1 场景一：修改默认引擎的处理器</h3>
        <div class="doc-scenario">
          <strong>适用场景</strong>
          <p>你用的是项目默认的 <code>renderMarkdown()</code> 函数，想修改默认行为。</p>
        </div>

        <div class="doc-step">
          <div class="doc-step-number">1</div>
          <div class="doc-step-content">
            <strong>找到默认处理器的文件位置</strong>
            <p><code>src/utils/chat/markdown/extensions/renderer/handlers/</code></p>
          </div>
        </div>

        <div class="doc-step">
          <div class="doc-step-number">2</div>
          <div class="doc-step-content">
            <strong>修改对应的 handler 文件</strong>
            <p>比如修改图片渲染，编辑 <code>image-handler.ts</code>：</p>
          </div>
        </div>

        <div class="doc-code-block">
          <pre><code>// src/utils/chat/markdown/extensions/renderer/handlers/image-handler.ts

import type { Tokens } from 'marked'
import { escapeHtml } from '../../utils/html-utils'

/**
 * 图片渲染处理器
 */
export function imageHandler(token: Tokens.Image): string {
  const { href, title, text } = token
  const titleAttr = title ? ` title="${escapeHtml(title)}"` : ''
  const altAttr = text ? ` alt="${escapeHtml(text)}"` : ''

  // 👇 这里改成你想要的 HTML 结构
  return `&lt;figure class="md-figure"&gt;
    &lt;img
      src="${href}"${altAttr}${titleAttr}
      loading="lazy"
      class="md-image"
      onclick="window.__openImagePreview__(this)"
    /&gt;
    ${text ? `&lt;figcaption&gt;${escapeHtml(text)}&lt;/figcaption&gt;` : ''}
  &lt;/figure&gt;`
}</code></pre>
        </div>

        <div class="doc-step">
          <div class="doc-step-number">3</div>
          <div class="doc-step-content">
            <strong>完成！</strong>
            <p>默认引擎会自动使用新的处理器，无需其他修改。</p>
          </div>
        </div>

        <h3>2.2 场景二：创建独立的引擎实例</h3>
        <div class="doc-scenario">
          <strong>适用场景</strong>
          <p>你需要创建一个独立的 Markdown 引擎，使用自己的处理器配置，不影响默认引擎。</p>
        </div>

        <div class="doc-step">
          <div class="doc-step-number">1</div>
          <div class="doc-step-content">
            <strong>在你需要的地方写代码</strong>
            <p>可以是 Vue 组件的 script 部分，也可以是单独的 .ts 文件。</p>
          </div>
        </div>

        <div class="doc-code-block">
          <pre><code>// 在你的 Vue 组件或 .ts 文件中

import {
  MarkdownEngine,
  createRendererExtension
} from '@/utils/chat/markdown'
import type { Tokens } from 'marked'

// 定义你的处理器
function myImageHandler(token: Tokens.Image): string {
  return `&lt;img
    src="${token.href}"
    alt="${token.text || ''}"
    class="my-custom-image"
    loading="lazy"
  /&gt;`
}

// 创建引擎
const engine = new MarkdownEngine()

// 创建渲染器扩展，传入你的处理器
const myRenderer = createRendererExtension({
  handlers: {
    image: myImageHandler
  }
})

// 注册扩展
engine.registerMarkedExtension(myRenderer)

// 必须调用 use() 使扩展生效
engine.use()

// 使用
const html = engine.render('# Hello')</code></pre>
        </div>

        <h3>2.3 可以覆盖哪些处理器？</h3>
        <div class="doc-table-wrapper">
          <table class="doc-table">
            <thead>
              <tr>
                <th>处理器</th>
                <th>对应的 Markdown</th>
                <th>Token 常用字段</th>
              </tr>
            </thead>
            <tbody>
              <tr><td><code>image</code></td><td><code>![alt](url)</code></td><td>href, text</td></tr>
              <tr><td><code>link</code></td><td><code>[text](url)</code></td><td>href, text</td></tr>
              <tr><td><code>code</code></td><td><code>```代码块```</code></td><td>text, lang</td></tr>
              <tr><td><code>heading</code></td><td><code># 标题</code></td><td>text, depth</td></tr>
              <tr><td><code>table</code></td><td>表格语法</td><td>header, rows</td></tr>
              <tr><td><code>list</code></td><td><code>- 列表</code></td><td>items, ordered</td></tr>
              <tr><td><code>listitem</code></td><td>列表项</td><td>text, task</td></tr>
            </tbody>
          </table>
        </div>

        <h3>2.4 同时覆盖多个处理器</h3>
        <div class="doc-code-block">
          <pre><code>const myRenderer = createRendererExtension({
  handlers: {
    image: (token) => `&lt;img src="${token.href}" loading="lazy" /&gt;`,

    link: (token) => {
      const isExternal = token.href.startsWith('http')
      return `&lt;a href="${token.href}" ${isExternal ? 'target="_blank"' : ''}&gt;${token.text}&lt;/a&gt;`
    }
  }
})</code></pre>
        </div>

        <h3>2.5 全局覆盖（高级）</h3>
        <p>如果你希望<strong>所有地方</strong>都使用你的处理器，可以直接操作全局注册表：</p>

        <div class="doc-code-block">
          <pre><code>import { globalHandlerRegistry } from '@/utils/chat/markdown'

// 这会影响所有后续创建的渲染器
if (globalHandlerRegistry.has('image')) {
  globalHandlerRegistry.register('image', (token) => {
    return `&lt;img src="${token.href}" class="global-style" /&gt;`
  })
}</code></pre>
        </div>

        <div class="doc-warning-list">
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>注意</strong>：全局覆盖会影响默认引擎，谨慎使用！</span>
          </div>
        </div>

        <!-- ========== 最佳实践 ========== -->
        <h2>三、最佳实践</h2>

        <h3>3.1 你需要创建独立引擎吗？</h3>
        <div class="doc-scenario">
          <strong>不需要创建独立引擎的情况（大多数）</strong>
          <ul>
            <li>只是想修改图片、代码块等元素的渲染方式 → 直接改 handler 文件</li>
            <li>想添加新的容器语法 → 创建 -extension.ts 文件，自动发现</li>
            <li>项目里用 <code>renderMarkdown()</code> 函数 → 默认引擎够用了</li>
          </ul>
        </div>

        <div class="doc-scenario">
          <strong>需要创建独立引擎的情况（少数）</strong>
          <ul>
            <li>你想在某个组件里用完全不同的渲染配置，不影响全局</li>
            <li>你需要多个不同的 Markdown 引擎实例</li>
          </ul>
        </div>

        <h3>3.2 常见问题</h3>
        <div class="doc-qa">
          <div class="doc-qa-item">
            <div class="doc-qa-q">Q: 我创建了新的容器扩展文件，为什么不生效？</div>
            <div class="doc-qa-a">
              A: 检查三点：<br>
              1. 文件名是否以 <code>-extension.ts</code> 结尾<br>
              2. 是否导出了 <code>extensionModule</code> 对象<br>
              3. <code>meta.enabled</code> 是否为 <code>true</code>
            </div>
          </div>
          <div class="doc-qa-item">
            <div class="doc-qa-q">Q: 我修改了 handler 文件，为什么不生效？</div>
            <div class="doc-qa-a">A: 可能是缓存问题，重启开发服务器试试：<code>pnpm dev</code></div>
          </div>
          <div class="doc-qa-item">
            <div class="doc-qa-q">Q: 容器内容里的 Markdown 语法没有被解析？</div>
            <div class="doc-qa-a">A: 你需要在 <code>setup</code> 函数里设置 <code>innerRender</code>，参考 1.5 节。</div>
          </div>
          <div class="doc-qa-item">
            <div class="doc-qa-q">Q: 自定义的 HTML 标签被过滤了？</div>
            <div class="doc-qa-a">A: 引擎默认启用 DOMPurify 做 XSS 防护。需要配置白名单。</div>
          </div>
        </div>

        <h3>3.3 注意事项</h3>
        <div class="doc-warning-list">
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>文件命名</strong>：容器扩展文件必须以 <code>-extension.ts</code> 结尾</span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>名称唯一</strong>：<code>meta.name</code> 不能重复</span>
          </div>
          <div class="doc-warning-item">
            <WarningOutlined class="doc-warning-icon" />
            <span><strong>必须 use()</strong>：创建独立引擎后，注册扩展必须调用 <code>engine.use()</code></span>
          </div>
        </div>

        <!-- ========== 快速参考 ========== -->
        <h2>四、速查表</h2>

        <h3>我想...</h3>
        <div class="doc-quick-ref">
          <div class="doc-quick-item">
            <div class="doc-quick-title">添加新的容器语法（如 :::mybox）</div>
            <div class="doc-quick-content">
              在 <code>extensions/container/</code> 目录下创建 <code>mybox-extension.ts</code>，
              实现 MarkdownExtension 接口并导出 <code>extensionModule</code>。
            </div>
          </div>
          <div class="doc-quick-item">
            <div class="doc-quick-title">修改图片渲染方式</div>
            <div class="doc-quick-content">
              直接修改 <code>extensions/renderer/handlers/image-handler.ts</code>。
            </div>
          </div>
          <div class="doc-quick-item">
            <div class="doc-quick-title">修改代码块渲染方式</div>
            <div class="doc-quick-content">
              直接修改 <code>extensions/renderer/handlers/code-handler.ts</code>。
            </div>
          </div>
          <div class="doc-quick-item">
            <div class="doc-quick-title">临时禁用某个容器扩展</div>
            <div class="doc-quick-content">
              在 <code>extensionModule.meta</code> 里设置 <code>enabled: false</code>。
            </div>
          </div>
          <div class="doc-quick-item">
            <div class="doc-quick-title">直接渲染 Markdown</div>
            <div class="doc-quick-content">
              <code>import { renderMarkdown } from '@/utils/chat/markdown'</code><br>
              <code>const html = renderMarkdown('# Hello')</code>
            </div>
          </div>
        </div>
      </article>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/doc/index.scss' as *;

.doc-sidebar-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

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
    border: 1px solid #52c41a;
  }

  .doc-warning-icon.success {
    color: #52c41a;
  }
}

.doc-scenario {
  margin: 1.5rem 0;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.08), rgba(24, 144, 255, 0.02));
  border-left: 4px solid #1890ff;
  border-radius: 0 8px 8px 0;

  strong {
    display: block;
    margin-bottom: 0.75rem;
    color: #1890ff;
    font-size: 1rem;
  }

  p {
    margin: 0;
    color: var(--text-color-secondary);
    line-height: 1.8;
  }

  ul {
    margin: 0.5rem 0 0 0;
    padding-left: 1.5rem;
    color: var(--text-color-secondary);

    li {
      margin-bottom: 0.5rem;
    }
  }

  pre {
    margin: 0.75rem 0 0 0;
    padding: 0.75rem 1rem;
    background: var(--bg-color-secondary);
    border-radius: 6px;
    font-size: 0.9rem;
  }
}

.doc-tip {
  margin: 1.25rem 0;
  padding: 1rem 1.25rem;
  background: rgba(250, 173, 20, 0.1);
  border: 1px solid rgba(250, 173, 20, 0.3);
  border-radius: 8px;

  strong {
    color: #d48806;
  }

  ul {
    margin: 0.5rem 0 0 0;
    padding-left: 1.5rem;
    color: var(--text-color-secondary);
  }
}

.doc-qa {
  margin: 1.5rem 0;
}

.doc-qa-item {
  margin-bottom: 1rem;
  padding: 1rem;
  background: var(--bg-color-secondary);
  border-radius: 8px;
}

.doc-qa-q {
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.doc-qa-a {
  color: var(--text-color-secondary);
  line-height: 1.8;
}

.doc-quick-ref {
  margin: 1rem 0;
}

.doc-quick-item {
  margin-bottom: 1rem;
  padding: 1rem;
  background: var(--bg-color-secondary);
  border-radius: 8px;
}

.doc-quick-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #52c41a;
}

.doc-quick-content {
  color: var(--text-color-secondary);
  font-size: 0.95rem;
}
</style>
