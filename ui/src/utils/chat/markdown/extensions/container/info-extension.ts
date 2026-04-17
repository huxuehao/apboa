/**
 * 描述：自定义容器扩展
 *
 * 支持 :::tip / :::warning / :::danger / :::info / :::success 语法
 *
 * @author huxuehao
 **/

import type { MarkdownExtension } from '../../core/types'
import type {
  InfoToken,
  InfoType,
  InfoExtensionConfig,
  ExtensionModule,
} from './types'
import {
  defaultInfoConfigs,
} from './types'
import { escapeHtml } from '@/utils/chat/markdown'

/**
 * 自定义容器扩展
 *
 * 支持语法：
 * :::tip 自定义标题
 * 内容
 * :::
 *
 * :::warning
 * 警告内容
 * :::
 */
export class InfoExtension implements MarkdownExtension {
  readonly name = 'info'
  readonly level = 'block' as const
  readonly priority = 60

  /**
   * 配置
   */
  private config: Required<InfoExtensionConfig>

  /**
   * 内部渲染函数引用（用于嵌套解析）
   */
  private innerRender: (text: string) => string

  /**
   * 构造函数
   *
   * @param config 扩展配置
   * @param innerRender 内部渲染函数（用于容器内容嵌套解析）
   */
  constructor(
    config: InfoExtensionConfig = {},
    innerRender: (text: string) => string = (text) => text
  ) {
    this.config = {
      enabledTypes: config.enabledTypes ??
        (Object.keys(defaultInfoConfigs) as InfoType[]),
      typeConfigs: config.typeConfigs ?? {},
      customRenderer: config.customRenderer ?? this.defaultRenderer.bind(this),
    }
    this.innerRender = innerRender
  }

  /**
   * 更新内部渲染函数
   *
   * @param render 渲染函数
   */
  setInnerRender(render: (text: string) => string): void {
    this.innerRender = render
  }

  /**
   * 定位函数：查找 ::: 的位置
   */
  start(src: string): number {
    return src.indexOf(':::')
  }

  /**
   * Tokenizer：解析 :::type 语法
   */
  tokenizer(src: string): InfoToken | undefined {
    // 构建类型正则
    const types = this.config.enabledTypes.join('|')
    const regex = new RegExp(`^:::(${types})(.*?)\\n([\\s\\S]*?):::\\s*(?:\\n|$)`)

    const match = src.match(regex)
    if (match) {
      return {
        type: 'info',
        raw: match[0],
        infoType: match[1] as InfoType,
        title: match[2]!.trim(),
        text: match[3]!.trim(),
      }
    }
    return undefined
  }

  /**
   * Renderer：渲染为 HTML
   */
  renderer(token: InfoToken | { type: string; raw: string; text?: string }): string {
    return this.config.customRenderer(token as InfoToken)
  }

  /**
   * 默认渲染函数
   *
   * @param token 容器 Token
   * @returns HTML 字符串
   */
  private defaultRenderer(token: InfoToken): string {
    const typeConfig = {
      ...defaultInfoConfigs[token.infoType],
      ...this.config.typeConfigs[token.infoType],
    }

    const icon = typeConfig.icon
    const title = token.title || typeConfig.defaultTitle
    const content = this.innerRender(token.text ?? '')

    return `<div class="md-info md-info-${token.infoType}">
      <p class="md-info-title">${icon} ${escapeHtml(title)}</p>
      <div class="md-info-content">${content}</div>
    </div>`
  }
}

/**
 * 创建容器扩展
 *
 * 工厂函数，用于快速创建容器扩展实例
 *
 * @param config 扩展配置
 * @param innerRender 内部渲染函数
 * @returns 容器扩展实例
 */
export function createInfoExtension(
  config?: InfoExtensionConfig,
  innerRender?: (text: string) => string
): InfoExtension {
  return new InfoExtension(config, innerRender)
}

/**
 * 默认容器扩展实例
 */
export const infoExtension = new InfoExtension()

/**
 * 容器扩展模块定义
 *
 * 用于自动发现和注册系统
 */
export const extensionModule: ExtensionModule<InfoExtension> = {
  meta: {
    name: 'info',
    description: '自定义容器扩展，支持 tip/warning/danger/info/success 语法',
    version: '1.0.0',
    author: 'huxuehao',
    enabled: true,
    priority: 60,
  },
  extension: infoExtension,
  /**
   * 初始化函数：设置内部渲染器
   *
   * @param engine Markdown 引擎实例
   */
  setup: (engine: unknown) => {
    // engine 参数由注册系统传入，用于设置嵌套渲染
    if (engine && typeof engine === 'object' && 'render' in engine) {
      infoExtension.setInnerRender((text) => (engine as { render: (t: string) => string }).render(text))
    }
  },
}
