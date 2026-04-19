# 二、渲染器处理器覆盖

:::info 这是干嘛的？
Markdown 渲染器会把 `![图片](url)` 变成 `<img src="url">`。
如果你想让图片变成别的样子（比如加 lazy loading、加点击预览），就需要覆盖 image 处理器。
:::

## 2.1 场景一：修改默认引擎的处理器

:::info 适用场景
你用的是项目默认的 `renderMarkdown()` 函数，想修改默认行为。
:::

**步骤 1：找到默认处理器的文件位置**

`src/utils/chat/markdown/extensions/renderer/handlers/`

**步骤 2：修改对应的 handler 文件**

比如修改图片渲染，编辑 `image-handler.ts`：

```typescript
// src/utils/chat/markdown/extensions/renderer/handlers/image-handler.ts

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
  return `<figure class="md-figure">
    <img
      src="${href}"${altAttr}${titleAttr}
      loading="lazy"
      class="md-image"
      onclick="window.__openImagePreview__(this)"
    />
    ${text ? `<figcaption>${escapeHtml(text)}</figcaption>` : ''}
  </figure>`
}
```

**步骤 3：完成！**

默认引擎会自动使用新的处理器，无需其他修改。

## 2.2 场景二：创建独立的引擎实例

:::info 适用场景
你需要创建一个独立的 Markdown 引擎，使用自己的处理器配置，不影响默认引擎。
:::

**步骤 1：在你需要的地方写代码**

可以是 Vue 组件的 script 部分，也可以是单独的 .ts 文件。

```typescript
// 在你的 Vue 组件或 .ts 文件中

import {
  MarkdownEngine,
  createRendererExtension
} from '@/utils/chat/markdown'
import type { Tokens } from 'marked'

// 定义你的处理器
function myImageHandler(token: Tokens.Image): string {
  return `<img
    src="${token.href}"
    alt="${token.text || ''}"
    class="my-custom-image"
    loading="lazy"
  />`
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
const html = engine.render('# Hello')
```

## 2.3 可以覆盖哪些处理器？

| 处理器 | 对应的 Markdown | Token 常用字段 |
|--------|-----------------|----------------|
| `image` | `![alt](url)` | href, text |
| `link` | `[text](url)` | href, text |
| `code` | ` ```代码块``` ` | text, lang |
| `heading` | `# 标题` | text, depth |
| `table` | 表格语法 | header, rows |
| `list` | `- 列表` | items, ordered |
| `listitem` | 列表项 | text, task |

## 2.4 同时覆盖多个处理器

```typescript
const myRenderer = createRendererExtension({
  handlers: {
    image: (token) => `<img src="${token.href}" loading="lazy" />`,

    link: (token) => {
      const isExternal = token.href.startsWith('http')
      return `<a href="${token.href}" ${isExternal ? 'target="_blank"' : ''}>${token.text}</a>`
    }
  }
})
```

## 2.5 全局覆盖（高级）

如果你希望**所有地方**都使用你的处理器，可以直接操作全局注册表：

```typescript
import { globalHandlerRegistry } from '@/utils/chat/markdown'

// 这会影响所有后续创建的渲染器
if (globalHandlerRegistry.has('image')) {
  globalHandlerRegistry.register('image', (token) => {
    return `<img src="${token.href}" class="global-style" />`
  })
}
```

:::warning 注意
全局覆盖会影响默认引擎，谨慎使用！
:::
