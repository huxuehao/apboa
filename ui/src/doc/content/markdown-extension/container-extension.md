# 一、容器扩展自动发现

## 1.1 工作原理

容器扩展支持自动发现机制：只需在 `utils/chat/markdown/extensions/container/` 目录下创建以 `-extension.ts` 结尾的文件，并导出 `extensionModule` 对象，扩展就会被自动发现和注册。

```
utils/chat/markdown/extensions/container/
├── index.ts                    # 自动发现逻辑
├── types.ts                    # 类型定义
├── info-extension.ts           # 容器扩展 ✓ 自动发现
└── your-extension.ts           # 你的扩展 ✓ 自动发现（需以 -extension.ts 结尾）
```

## 1.2 快速开始

创建一个新的扩展只需两步：

**步骤 1：创建扩展文件**

在 `utils/chat/markdown/extensions/container/` 目录下创建 `xxx-extension.ts` 文件：

```typescript
// notice-extension.ts
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
    return `<div class="md-notice">
      <p class="md-notice-title">📢 ${title}</p>
      <div class="md-notice-content">${token.text ?? ''}</div>
    </div>`
  }
}

const noticeExtension = new NoticeExtension()

/**
 * 导出扩展模块 - 自动发现的关键！
 */
export const extensionModule: ExtensionModule<NoticeExtension> = {
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
}
```

**步骤 2：完成！**

扩展会自动被发现和注册，无需手动导入。

## 1.3 扩展模块接口详解

`extensionModule` 是自动发现机制的核心，它告诉系统你的扩展长什么样、怎么用：

```typescript
// 你必须导出这个对象
export const extensionModule: ExtensionModule<MyExtension> = {
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
}
```

:::tip 关键点
- `meta.name` 必须唯一，重复会报错
- `meta.enabled = false` 可以临时禁用扩展，无需删除文件
- `meta.priority` 影响解析顺序，数字越小越先执行
:::

## 1.4 文件命名约定

:::warning 命名规则
- ✅ **正确**：`tip-extension.ts`、`notice-extension.ts`
- ❌ **错误**：`tip.ts`、`notice.ts`（不会被自动发现）
:::

## 1.5 高级用法：支持嵌套 Markdown

:::info 场景说明
假设你的容器内容包含 Markdown 语法，你需要按照下面的方法实现内部渲染器。
:::

解决方法：在 `setup` 函数中设置内部渲染器：

```typescript
class NoticeExtension implements MarkdownExtension {
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
    return `<div class="md-notice">${content}</div>`
  }
}

const noticeExtension = new NoticeExtension()

// 4. 在 setup 中设置内部渲染器
export const extensionModule: ExtensionModule<NoticeExtension> = {
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
}
```

:::tip 简单理解
把容器内容交给 Markdown 引擎再渲染一遍。
:::

## 1.6 进阶：手动控制扩展加载

:::info 什么时候需要手动控制？
- 你想排除某些扩展不加载
- 你想只加载特定扩展
- 你不用默认引擎，而是创建自己的引擎实例
:::

使用 `createAutoLoader` 手动控制：

```typescript
import {
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
engine.use()
```

:::tip 提示
大多数情况下你不需要这个！默认引擎已经自动加载所有扩展。只有特殊需求才需要手动控制。
:::
