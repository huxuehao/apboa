/**
 * 可扩展标签系统核心工具
 * 支持解析、序列化及渲染器注册
 *
 * @author huxuehao
 */

import type { Component } from 'vue'

/**
 * 解析后的内容段类型
 */
export interface ParsedSegment {
  type: 'text' | 'tag'
  content?: string
  tagName?: string
  tagContent?: string
}

/**
 * 标签渲染器定义
 */
export interface TagRendererDefinition {
  tagName: string
  component: Component
}

/**
 * 标签渲染器注册表
 * 支持后续动态注册新标签类型
 */
class TagRegistryClass {
  private renderers = new Map<string, Component>()

  /**
   * 注册标签渲染器
   * @param definition 标签渲染器定义
   */
  register(definition: TagRendererDefinition): void {
    this.renderers.set(definition.tagName, definition.component)
  }

  /**
   * 获取标签对应的渲染组件
   * @param tagName 标签名称
   * @returns 渲染组件或 undefined
   */
  get(tagName: string): Component | undefined {
    return this.renderers.get(tagName)
  }

  /**
   * 判断标签是否已注册
   * @param tagName 标签名称
   */
  has(tagName: string): boolean {
    return this.renderers.has(tagName)
  }
}

export const TagRegistry = new TagRegistryClass()

/**
 * 解析带标签标记的文本为段数组
 * 匹配格式：<tagname>content</tagname>
 *
 * @param text 原始文本
 * @returns ParsedSegment[]
 */
export function parseTaggedContent(text: string): ParsedSegment[] {
  if (!text) return []

  const segments: ParsedSegment[] = []
  // 匹配成对标签：开标签 + 内容 + 闭标签
  const regex = /<([a-zA-Z][a-zA-Z0-9\-]*)>([\s\S]*?)<\/\1>/g

  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(text)) !== null) {
    const matchStart = match.index
    const matchEnd = regex.lastIndex

    // 捕获标签前的文本
    if (matchStart > lastIndex) {
      segments.push({
        type: 'text',
        content: text.slice(lastIndex, matchStart)
      })
    }

    // 捕获标签段
    segments.push({
      type: 'tag',
      tagName: match[1],
      tagContent: match[2],
      content: match[0]
    })

    lastIndex = matchEnd
  }

  // 捕获剩余文本
  if (lastIndex < text.length) {
    segments.push({
      type: 'text',
      content: text.slice(lastIndex)
    })
  }

  return segments
}

/**
 * 将段数组序列化为带标签标记的文本
 *
 * @param segments 段数组
 * @returns 序列化后的文本
 */
export function serializeTaggedContent(segments: ParsedSegment[]): string {
  return segments
    .map((seg) => {
      if (seg.type === 'tag' && seg.tagName && seg.tagContent !== undefined) {
        return `<${seg.tagName}>${seg.tagContent}</${seg.tagName}>`
      }
      return seg.content ?? ''
    })
    .join('')
}

/**
 * 从 contenteditable DOM 中提取带标签标记的文本
 * 将 data-tag / data-content 属性的元素序列化为 <tag>content</tag>
 *
 * @param root DOM 根元素
 * @returns 序列化后的文本
 */
export function extractTextFromEditor(root: HTMLElement): string {
  let result = ''

  function traverse(node: Node): void {
    if (node.nodeType === Node.TEXT_NODE) {
      result += node.textContent ?? ''
      return
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement

      // 识别标签元素
      const tagName = el.getAttribute('data-tag')
      const tagContent = el.getAttribute('data-content')
      if (tagName && tagContent !== null) {
        result += `<${tagName}>${tagContent}</${tagName}>`
        return
      }

      // div / p 块级元素需要换行
      if (el.tagName === 'DIV' || el.tagName === 'P') {
        if (result && !result.endsWith('\n')) {
          // 子节点处理前不加换行，处理完块级元素后加
        }
        for (const child of Array.from(el.childNodes)) {
          traverse(child)
        }
        if (!result.endsWith('\n')) {
          result += '\n'
        }
        return
      }

      // br 标签
      if (el.tagName === 'BR') {
        result += '\n'
        return
      }

      // 其他元素递归处理子节点
      for (const child of Array.from(el.childNodes)) {
        traverse(child)
      }
    }
  }

  for (const child of Array.from(root.childNodes)) {
    traverse(child)
  }

  // 移除末尾多余的换行
  return result.replace(/\n+$/, '')
}

/**
 * 将带标签标记的文本渲染为 HTML 字符串（用于初始化 contenteditable）
 *
 * @param text 带标签标记的文本
 * @param tagRenderFn 标签渲染函数，接收 tagName 和 tagContent，返回 HTML 字符串
 * @returns HTML 字符串
 */
export function renderTaggedTextToHtml(
  text: string,
  tagRenderFn: (tagName: string, tagContent: string) => string
): string {
  const segments = parseTaggedContent(text)
  return segments
    .map((seg) => {
      if (seg.type === 'tag' && seg.tagName && seg.tagContent !== undefined) {
        return tagRenderFn(seg.tagName, seg.tagContent)
      }
      // 转义 HTML 特殊字符
      return (seg.content ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>')
    })
    .join('')
}
