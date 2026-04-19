# 四、速查表

| 我想... | 怎么做 |
|---------|--------|
| 添加新的容器语法（如 :::mybox） | 在 `extensions/container/` 目录下创建 `mybox-extension.ts`，实现 MarkdownExtension 接口并导出 `extensionModule`。 |
| 修改图片渲染方式 | 直接修改 `extensions/renderer/handlers/image-handler.ts`。 |
| 修改代码块渲染方式 | 直接修改 `extensions/renderer/handlers/code-handler.ts`。 |
| 临时禁用某个容器扩展 | 在 `extensionModule.meta` 里设置 `enabled: false`。 |
| 直接渲染 Markdown | `import { renderMarkdown } from '@/utils/chat/markdown'` → `const html = renderMarkdown('# Hello')` |
