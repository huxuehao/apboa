/**
 * 描述：示例扩展 - 演示如何创建新的容器扩展
 *
 * 这个文件展示了如何创建一个新的扩展并自动注册到 Markdown 引擎
 * 只需要在 container 目录下创建文件并导出 extensionModule 即可
 *
 * @author huxuehao
 **/

import type { MarkdownExtension, CustomToken } from '../../core/types'
import type { ExtensionModule } from './types'

/**
 * 示例扩展类
 *
 * 这是一个演示用的扩展，支持 :::example 语法
 */
class ExampleExtension implements MarkdownExtension {
  readonly name = 'example'
  readonly level = 'block' as const
  readonly priority = 70

  start(src: string): number {
    return src.indexOf(':::example')
  }

  tokenizer(src: string): CustomToken | undefined {
    const match = src.match(/^:::example\n([\s\S]*?):::\s*(?:\n|$)/)
    if (match) {
      return {
        type: 'example',
        raw: match[0],
        text: match[1]!.trim(),
      } as CustomToken
    }
    return undefined
  }

  renderer(token: CustomToken): string {
    return `<div class="md-example">
      <p class="md-example-title">示例</p>
      <div class="md-example-content">${token.text ?? ''}</div>
    </div>`
  }
}

/**
 * 示例扩展实例
 */
const exampleExtension = new ExampleExtension()

/**
 * 扩展模块定义
 *
 * 导出此对象后，扩展会被自动发现和注册
 */
export const extensionModule: ExtensionModule<ExampleExtension> = {
  meta: {
    name: 'example',
    description: '示例扩展，演示自动注册机制',
    version: '1.0.0',
    author: 'huxuehao',
    enabled: false, // 设置为 false 禁用此示例扩展
    priority: 70,
  },
  extension: exampleExtension,
  // 可选：初始化函数
  setup: () => {
    console.log('[ExampleExtension] 扩展已加载')
  },
}

// 导出扩展类供外部使用（可选）
export { ExampleExtension, exampleExtension }
