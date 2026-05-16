<script setup lang="ts">
/**
 * 聊天输入框 contenteditable 编辑器组件
 * 内聚 contenteditable 渲染、@mention 检测与下拉、标签插入与删除、IME、粘贴、键盘等核心交互
 *
 * @component
 */
import { computed, nextTick, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import ResourceMentionDropdown from './ResourceMentionDropdown.vue'
import { type FlatFileItem, useWorkspaceFiles } from '@/composables/chat/useWorkspaceFiles'
import { extractTextFromEditor, renderTaggedTextToHtml } from '@/utils/chat/tagSystem'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    sessionId?: string | null
    mentionAllowed?: boolean
    isRunning?: boolean
  }>(),
  {
    placeholder: '输入消息...',
    sessionId: null,
    mentionAllowed: false,
    isRunning: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
  (e: 'inputTagPreview', value: FlatFileItem): void
}>()

const editorRef = ref<HTMLDivElement | null>()
const dropdownRef = ref<InstanceType<typeof ResourceMentionDropdown> | null>()

/** 记录最后一次 emit 的值，用于区分内外部更新 */
const lastEmittedValue = ref(props.modelValue)
/** 是否正在输入法组合中 */
const isComposing = ref(false)
/** @mention 下拉显示状态 */
const mentionVisible = ref(false)
/** @mention 查询关键词 */
const mentionQuery = ref('')

/** 工作空间文件数据 */
const sessionIdRef = computed(() => props.sessionId ?? null)
const { flatFiles, fetchFiles } = useWorkspaceFiles(sessionIdRef)

/**
 * 判断编辑器是否有内容（含纯文本或标签）
 */
const hasEditorContent = computed(() => {
  const editor = editorRef.value
  if (!editor) return false
  const textContent = editor.textContent?.trim() !== ''
  const hasTags = editor.querySelectorAll('[data-tag]').length > 0
  return textContent || hasTags
})

/**
 * HTML 字符转义
 *
 * @param str 原始字符串
 * @return 转义后的字符串
 */
const escapeHtml = (str: string): string => {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * 渲染标签为 HTML 字符串
 *
 * @param tagName 标签名
 * @param tagContent 标签内容
 * @return 渲染后的 HTML
 */
const renderTagToHtml = (tagName: string, tagContent: string): string => {
  if (tagName === 'workspace-file') {
    const name = tagContent.split('/').pop() || tagContent
    return `<span contenteditable="false" class="editor-tag editor-tag-${tagName}" data-tag="${tagName}" data-content="${escapeHtml(tagContent)}"><span class="editor-tag-inner"><span class="editor-tag-name">${escapeHtml(name)}</span></span></span>`
  }
  // 未知标签，显示原始文本
  return escapeHtml(`<${tagName}>${tagContent}</${tagName}>`)
}

/**
 * 将 modelValue 渲染到 contenteditable 中
 *
 * @param text 待渲染文本
 */
const renderEditorContent = (text: string) => {
  if (!editorRef.value) return
  editorRef.value.innerHTML = renderTaggedTextToHtml(text, renderTagToHtml)
}

/**
 * 从编辑器提取内容并 emit 更新
 */
const emitContentUpdate = () => {
  if (!editorRef.value) return
  const content = extractTextFromEditor(editorRef.value)
  if (content !== lastEmittedValue.value) {
    lastEmittedValue.value = content
    emit('update:modelValue', content)
  }
}

/**
 * 自动调整编辑器高度（受 max-height 限制）
 */
const autoResize = () => {
  const el = editorRef.value
  if (!el) return
  el.style.height = 'auto'
  const maxHeight = 300
  el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`
}

/**
 * 检查并清理编辑器空状态，确保 :empty 伪类生效
 */
const sanitizeEmptyEditor = () => {
  const editor = editorRef.value
  if (!editor) return

  const textContent = editor.textContent || ''
  const hasTags = editor.querySelectorAll('[data-tag]').length > 0
  const hasTextContent = textContent.trim() !== ''
  const isEmpty = !hasTextContent && !hasTags

  if (isEmpty) {
    // 完全清空，确保 :empty 伪类能生效
    editor.innerHTML = ''
  } else if (!hasTextContent && hasTags) {
    // 只有标签，没有文本内容，移除空白文本节点
    const walker = document.createTreeWalker(editor, NodeFilter.SHOW_TEXT)
    const textNodesToRemove: Text[] = []
    while (walker.nextNode()) {
      const textNode = walker.currentNode as Text
      if (textNode.textContent?.trim() === '') {
        textNodesToRemove.push(textNode)
      }
    }
    textNodesToRemove.forEach((node) => node.remove())
  }
}

/**
 * 检测当前光标位置是否触发 @mention
 */
const checkMentionTrigger = () => {
  if (!props.mentionAllowed) {
    return
  }

  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) {
    mentionVisible.value = false
    return
  }

  const range = sel.getRangeAt(0)
  const node = range.startContainer
  const offset = range.startOffset

  // 仅当光标在文本节点中时检测
  if (node.nodeType !== Node.TEXT_NODE) {
    mentionVisible.value = false
    return
  }

  const text = node.textContent || ''
  const textBeforeCursor = text.slice(0, offset)

  // 向前查找最近的 @
  const atIndex = textBeforeCursor.lastIndexOf('@')
  if (atIndex === -1) {
    mentionVisible.value = false
    return
  }

  // 检查 @ 前面是否是空格或行首
  const charBeforeAt = textBeforeCursor.charAt(atIndex - 1)
  if (atIndex > 0 && charBeforeAt !== ' ') {
    mentionVisible.value = false
    return
  }

  // 提取查询词（@ 之后到光标）
  mentionQuery.value = textBeforeCursor.slice(atIndex + 1)
  mentionVisible.value = true
  fetchFiles()
}

/**
 * 在编辑器中查找包含 @mention 触发符的文本节点（不依赖 selection）
 *
 * @return 命中信息或 null
 */
const findMentionAtInEditor = (): { textNode: Text; atIndex: number } | null => {
  const editor = editorRef.value
  if (!editor) return null

  // 使用 TreeWalker 遍历所有文本节点，查找最后一个合法的 @
  const walker = document.createTreeWalker(editor, NodeFilter.SHOW_TEXT)
  let lastMatch: { textNode: Text; atIndex: number } | null = null
  let currentNode: Node | null
  while ((currentNode = walker.nextNode())) {
    const text = currentNode.textContent || ''
    // 从后向前查找满足条件的 @
    for (let i = text.length - 1; i >= 0; i--) {
      if (text[i] === '@' && (i === 0 || text[i - 1] === ' ')) {
        lastMatch = { textNode: currentNode as Text, atIndex: i }
        break
      }
    }
  }
  return lastMatch
}

/**
 * 插入工作空间文件标签
 *
 * @param item 文件项
 */
const insertWorkspaceFileTag = (item: FlatFileItem) => {
  const path = item.path
  const editor = editorRef.value
  if (!editor) {
    mentionVisible.value = false
    return
  }

  // 通过 TreeWalker 查找 @ 位置（不依赖 selection）
  const match = findMentionAtInEditor()
  if (!match) {
    mentionVisible.value = false
    return
  }

  const { textNode, atIndex } = match
  const text = textNode.textContent || ''

  // @ 及查询词的总长度 = 1( @) + mentionQuery.length
  const queryLen = mentionQuery.value.length
  const beforeAt = text.slice(0, atIndex)
  const afterMention = text.slice(atIndex + 1 + queryLen)

  // 更新文本节点为 @ 之前的内容
  textNode.textContent = beforeAt

  // 创建标签元素
  const tagEl = document.createElement('span')
  tagEl.contentEditable = 'false'
  tagEl.className = 'editor-tag editor-tag-workspace-file'
  tagEl.setAttribute('data-tag', 'workspace-file')
  tagEl.setAttribute('data-content', path)

  const name = path.split('/').pop() || path
  tagEl.innerHTML = `<span class="editor-tag-inner"><span class="editor-tag-name">${escapeHtml(name)}</span></span>`

  // 添加点击事件监听（预览）
  const innerSpan = tagEl.querySelector('.editor-tag-inner')
  innerSpan?.addEventListener('click', () => emit('inputTagPreview', item))

  const parent = textNode.parentNode!
  parent.insertBefore(tagEl, textNode.nextSibling)

  // 插入查询词之后的文本
  if (afterMention) {
    const afterText = document.createTextNode(afterMention)
    parent.insertBefore(afterText, tagEl.nextSibling)
  }

  // 将光标移到标签后面
  const sel = window.getSelection()
  if (sel) {
    const newRange = document.createRange()
    newRange.setStartAfter(tagEl)
    newRange.collapse(true)
    sel.removeAllRanges()
    sel.addRange(newRange)
  }

  // 如果文本节点内容为空，移除它
  if (!beforeAt) {
    parent.removeChild(textNode)
  }

  mentionVisible.value = false
  mentionQuery.value = ''

  nextTick(() => {
    emitContentUpdate()
    autoResize()
    sanitizeEmptyEditor()
  })
}

/**
 * 判断节点是否为标签元素
 */
const isTagElement = (node: Node): node is HTMLElement => {
  return (
    node.nodeType === Node.ELEMENT_NODE &&
    (node as HTMLElement).hasAttribute('data-tag')
  )
}

/**
 * 处理 editor input 事件
 */
const handleEditorInput = () => {
  if (isComposing.value) return
  sanitizeEmptyEditor()
  emitContentUpdate()
  nextTick(() => {
    checkMentionTrigger()
    autoResize()
  })
}

/**
 * 处理 editor keydown 事件，含 Enter 发送、Backspace/Delete 整块删除标签等逻辑
 *
 * @param e 键盘事件
 */
const handleEditorKeydown = (e: KeyboardEvent) => {
  // 下拉打开时，优先让下拉处理键盘导航
  if (mentionVisible.value) {
    if (['ArrowUp', 'ArrowDown', 'Enter', 'Escape'].includes(e.key)) {
      dropdownRef.value?.handleKeydown(e)
      return
    }
  }

  // 移动端不处理 Enter 发送
  const isMobile = window.innerWidth <= 768
  if (!isMobile && e.key === 'Enter' && !e.shiftKey) {
    if (props.isRunning) {
      message.info('停止生成后再发送')
      return
    }
    e.preventDefault()
    emit('send')
    return
  }

  // 标签整块删除
  if (e.key === 'Backspace' || e.key === 'Delete') {
    const sel = window.getSelection()
    if (!sel || sel.rangeCount === 0) return

    const range = sel.getRangeAt(0)
    if (!range.collapsed) return // 有选区时走默认行为

    const node = range.startContainer
    const offset = range.startOffset

    if (e.key === 'Backspace' && offset === 0) {
      // 光标在文本节点开头，检查前一个兄弟节点是否是标签
      const prev = node.previousSibling
      if (prev && isTagElement(prev)) {
        e.preventDefault()
        prev.remove()
        emitContentUpdate()
        autoResize()
        sanitizeEmptyEditor()
        return
      }
      // 如果前一个节点是元素（非标签），也检查它的最后一个子节点链
      if (node.nodeType === Node.TEXT_NODE) {
        const parent = node.parentNode
        if (parent && parent !== editorRef.value) {
          const parentPrev = parent.previousSibling
          if (parentPrev && isTagElement(parentPrev)) {
            e.preventDefault()
            parentPrev.remove()
            emitContentUpdate()
            autoResize()
            sanitizeEmptyEditor()
            return
          }
        }
      }
    }

    if (e.key === 'Delete') {
      let next: Node | null = null

      if (node.nodeType === Node.TEXT_NODE) {
        const textLen = node.textContent?.length || 0
        if (offset >= textLen) {
          next = node.nextSibling
        }
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        next = node.childNodes[offset] || null
      }

      if (next && isTagElement(next)) {
        e.preventDefault()
        next.remove()
        emitContentUpdate()
        autoResize()
        sanitizeEmptyEditor()
        return
      }
    }
  }

  // 删除后检查并清理空状态
  nextTick(() => {
    sanitizeEmptyEditor()
  })
}

/**
 * 处理粘贴事件，仅保留纯文本，并将换行转换为 <br>
 *
 * @param e 剪贴板事件
 */
const handleEditorPaste = (e: ClipboardEvent) => {
  e.preventDefault()

  let text = e.clipboardData?.getData('text/plain') || ''

  // 如果粘贴的是 HTML，也提取纯文本
  if (!text) {
    const html = e.clipboardData?.getData('text/html')
    if (html) {
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = html
      text = tempDiv.textContent || tempDiv.innerText || ''
    }
  }

  if (!text) return

  const selection = window.getSelection()
  if (!selection || !selection.rangeCount) return

  const range = selection.getRangeAt(0)

  if (!range.collapsed) {
    range.deleteContents()
  }

  const fragment = document.createDocumentFragment()
  const lines = text.split('\n')

  lines.forEach((line, index) => {
    if (line) {
      fragment.appendChild(document.createTextNode(line))
    }
    if (index < lines.length - 1) {
      fragment.appendChild(document.createElement('br'))
    }
  })

  if (fragment.childNodes.length === 0) {
    fragment.appendChild(document.createTextNode('\u200B'))
  }

  range.insertNode(fragment)

  const lastNode = fragment.lastChild
  if (lastNode) {
    if (lastNode.nodeType === Node.TEXT_NODE) {
      range.setStart(lastNode, lastNode.textContent?.length || 0)
    } else {
      range.setStartAfter(lastNode)
    }
    range.collapse(true)
  } else {
    range.collapse(false)
  }

  selection.removeAllRanges()
  selection.addRange(range)

  nextTick(() => {
    sanitizeEmptyEditor()
    emitContentUpdate()
    autoResize()
  })
}

/**
 * 输入法组合开始
 */
const handleCompositionStart = () => {
  isComposing.value = true
}

/**
 * 输入法组合结束
 */
const handleCompositionEnd = () => {
  isComposing.value = false
  emitContentUpdate()
  nextTick(() => {
    checkMentionTrigger()
    autoResize()
  })
}

/**
 * 编辑器失去焦点时关闭 mention 下拉并清理空状态
 */
const handleEditorBlur = () => {
  mentionVisible.value = false
  nextTick(() => {
    sanitizeEmptyEditor()
  })
}

/**
 * 远程触发 @mention：聚焦编辑器、必要时插入空格、插入 @ 并显示下拉
 */
const triggerMention = async () => {
  const editor = editorRef.value
  if (!editor) return

  editor.focus()
  await nextTick()

  const sel = window.getSelection()
  if (!sel) return

  // 如果 selection 不在 editor 内，将光标移到末尾
  if (sel.rangeCount === 0 || !editor.contains(sel.anchorNode)) {
    const range = document.createRange()
    range.selectNodeContents(editor)
    range.collapse(false)
    sel.removeAllRanges()
    sel.addRange(range)
  }

  const range = sel.getRangeAt(0)

  // 判断光标前是否需要加空格
  let needSpace = true
  const node = range.startContainer
  if (node.nodeType === Node.TEXT_NODE) {
    const offset = range.startOffset
    const text = node.textContent || ''
    if (offset === 0 || text.charAt(offset - 1) === ' ' || text.charAt(offset - 1) === '\n') {
      needSpace = false
    }
  } else if (editor.childNodes.length === 0) {
    needSpace = false
  }

  const insertText = needSpace ? ' @' : '@'
  const textNode = document.createTextNode(insertText)
  range.insertNode(textNode)

  range.setStartAfter(textNode)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)

  await nextTick()

  emitContentUpdate()

  await nextTick()

  checkMentionTrigger()

  // 如果下拉没有显示，强制显示（兜底方案）
  if (!mentionVisible.value && insertText.includes('@')) {
    mentionQuery.value = ''
    mentionVisible.value = true
    fetchFiles()
  }

  autoResize()
}

/**
 * 暴露给父组件的能力
 */
defineExpose({
  triggerMention,
  focus: () => editorRef.value?.focus()
})

// 监听外部 modelValue 变化，重新渲染 editor
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal === lastEmittedValue.value) return
    renderEditorContent(newVal)
    lastEmittedValue.value = newVal
    nextTick(() => {
      autoResize()
      sanitizeEmptyEditor()
    })
  },
  { immediate: true }
)
</script>

<template>
  <div class="chat-input-editor-wrapper">
    <div
      ref="editorRef"
      :data-placeholder="modelValue ? '' : placeholder"
      contenteditable="true"
      class="chat-input-editor"
      :class="{ 'has-content': hasEditorContent }"
      @input="handleEditorInput"
      @keydown="handleEditorKeydown"
      @paste="handleEditorPaste"
      @compositionstart="handleCompositionStart"
      @compositionend="handleCompositionEnd"
      @blur="handleEditorBlur"
    />
    <ResourceMentionDropdown
      ref="dropdownRef"
      :visible="mentionVisible"
      :items="flatFiles"
      :keyword="mentionQuery"
      @select="insertWorkspaceFileTag"
      @close="mentionVisible = false"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.chat-input-editor-wrapper {
  position: relative;
  flex: 1;
  min-height: 0;
}

.chat-input-editor {
  position: relative;
  flex: 1;
  min-height: 60px;
  max-height: 300px;
  overflow-y: auto;
  border: none;
  outline: none;
  font-size: var(--font-size-base);
  line-height: 1.5;
  color: var(--color-text-primary);
  background: transparent;
  padding: 5px 0;
  word-break: break-word;
  white-space: pre-wrap;
  text-align: left;

  // 使用 :before 伪元素作为 placeholder
  &:empty::before,
  &:not(.has-content):not(:focus)::before {
    content: attr(data-placeholder);
    position: absolute;
    top: 5px;
    left: 0;
    right: 0;
    color: var(--color-text-placeholder);
    pointer-events: none;
  }

  // 有内容时隐藏 placeholder
  &.has-content::before {
    content: none !important;
  }

  // 当有标签但没有文本内容时，也不显示 placeholder
  &:has([data-tag]):not(:has(:not([data-tag]):not(br)))::before {
    content: none !important;
  }
}

/* contenteditable 内标签样式 */
:deep(.editor-tag) {
  display: inline;
  user-select: none;
  cursor: default;
}

:deep(.editor-tag-inner) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 6px;
  background: rgba(15, 116, 255, 0.1);
  border-radius: 4px;
  font-size: 13px;
  color: #0F74FF;
  vertical-align: middle;
  white-space: nowrap;
  margin: 0 2px;
  cursor: pointer;
}

:deep(.editor-tag-name) {
  font-weight: 500;
}
</style>
