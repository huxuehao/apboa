/**
 * 描述：自定义容器扩展
 *
 * 支持 :::tip / :::warning / :::danger / :::info / :::success 语法
 *
 * @author huxuehao
 **/

import type { MarkdownExtension } from '../../core/types'
import type {
  ContainerToken,
  ContainerType,
  ContainerExtensionConfig,
} from './types'
import {
  defaultContainerConfigs,
} from './types'
import { escapeHtml } from '../../utils/html-utils'

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
export class ContainerExtension implements MarkdownExtension {
  readonly name = 'container'
  readonly level = 'block' as const
  readonly priority = 60

  /**
   * 配置
   */
  private config: Required<ContainerExtensionConfig>

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
    config: ContainerExtensionConfig = {},
    innerRender: (text: string) => string = (text) => text
  ) {
    this.config = {
      enabledTypes: config.enabledTypes ??
        (Object.keys(defaultContainerConfigs) as ContainerType[]),
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
  tokenizer(src: string): ContainerToken | undefined {
    // 构建类型正则
    const types = this.config.enabledTypes.join('|')
    const regex = new RegExp(`^:::(${types})(.*?)\\n([\\s\\S]*?):::\\s*(?:\\n|$)`)

    const match = src.match(regex)
    if (match) {
      return {
        type: 'container',
        raw: match[0],
        containerType: match[1] as ContainerType,
        title: match[2]!.trim(),
        text: match[3]!.trim(),
      }
    }
    return undefined
  }

  /**
   * Renderer：渲染为 HTML
   */
  renderer(token: ContainerToken | { type: string; raw: string; text?: string }): string {
    return this.config.customRenderer(token as ContainerToken)
  }

  /**
   * 默认渲染函数
   *
   * @param token 容器 Token
   * @returns HTML 字符串
   */
  private defaultRenderer(token: ContainerToken): string {
    const typeConfig = {
      ...defaultContainerConfigs[token.containerType],
      ...this.config.typeConfigs[token.containerType],
    }

    const icon = typeConfig.icon
    const title = token.title || typeConfig.defaultTitle
    const content = this.innerRender(token.text ?? '')

    return `<div class="md-container md-container-${token.containerType}">
      <p class="md-container-title">${icon} ${escapeHtml(title)}</p>
      <div class="md-container-content">${content}</div>
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
export function createContainerExtension(
  config?: ContainerExtensionConfig,
  innerRender?: (text: string) => string
): ContainerExtension {
  return new ContainerExtension(config, innerRender)
}

/**
 * 默认容器扩展实例
 */
export const containerExtension = new ContainerExtension()
