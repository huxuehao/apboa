<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  LoadingOutlined,
  PaperClipOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import * as attachApi from '@/api/attach'
import MediaPreview from '@/components/common/MediaPreview.vue'
import MediaIcon from '@/components/common/MediaIcon.vue'
import ResourceMentionDropdown from './ResourceMentionDropdown.vue'
import { type FlatFileItem, useWorkspaceFiles } from '@/composables/chat/useWorkspaceFiles'
import { extractTextFromEditor, renderTaggedTextToHtml } from '@/utils/chat/tagSystem'
import type {UploadedFileItem} from '@/types'

const props = withDefaults(
  defineProps<{
    modelValue: string
    uploadedFiles?: UploadedFileItem[]
    isRunning?: boolean
    placeholder?: string
    memoryActive?: boolean
    planActive?: boolean
    enableMemory?: boolean
    enablePlanning?: boolean
    toolProcessActive?: boolean
    showToolProcess?: boolean
    allowUploadFileType?: string[]
    sessionId?: string | null
    workspacePanelOpen?: boolean
  }>(),
  {
    uploadedFiles: () => [],
    memoryActive: false,
    planActive: false,
    enableMemory: false,
    enablePlanning: false,
    toolProcessActive: false,
    showToolProcess: false,
    sessionId: null,
    workspacePanelOpen: false
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:uploadedFiles', value: UploadedFileItem[]): void
  (e: 'send'): void
  (e: 'abort'): void
  (e: 'memory', value: boolean): void
  (e: 'plan', value: boolean): void
  (e: 'toolProcess', value: boolean): void
  (e: 'inputTagPreview', value: FlatFileItem): void
}>()

const editorRef = ref<HTMLDivElement | null>()
const fileInputRef = ref<HTMLInputElement | null>()
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

/** 格式化文件大小显示 */
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

/** 从文件名解析扩展名（小写） */
const getExtension = (fileName: string): string => {
  const lastDot = fileName.lastIndexOf('.')
  return lastDot > -1 ? fileName.slice(lastDot + 1).toLowerCase() : ''
}

// 预览相关状态
const previewVisible = ref(false)
const previewCurrentIndex = ref(0)

/**
 * 判断编辑器是否有内容
 */
const hasEditorContent = computed(() => {
  const editor = editorRef.value
  if (!editor) return false
  const textContent = editor.textContent?.trim() !== ''
  const hasTags = editor.querySelectorAll('[data-tag]').length > 0
  return textContent || hasTags
})

/**
 * 打开文件预览
 */
const openPreview = (item: UploadedFileItem, index: number) => {
  if (item.uploading) {
    message.warning('文件上传中')
    return
  }
  previewCurrentIndex.value = index
  previewVisible.value = true
}

/** 检查文件类型是否在允许列表中 */
const isFileTypeAllowed = (extension: string): boolean => {
  const allowed = props.allowUploadFileType
  if (!allowed?.length) return true
  return allowed.some((t) => t.toLowerCase() === extension)
}

/** 根据 allowUploadFileType 生成 input accept 属性值 */
const fileAcceptAttr = (): string => {
  const allowed = props.allowUploadFileType
  if (!allowed?.length) return '*/*'
  return allowed.map((t) => `.${t}`).join(',')
}

const toggleMemory = () => {
  if (!props.enableMemory) return
  emit('memory', !props.memoryActive)
}
const toggleToolProcess = () => {
  if (!props.showToolProcess) return
  emit('toolProcess', !props.toolProcessActive)
}
const handleFileClick = () => {
  fileInputRef.value?.click()
}
const handleFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files?.length) {
    input.value = ''
    return
  }
  const fileArray = Array.from(files)
  const allowedFiles: File[] = []
  const rejectedNames: string[] = []
  for (const file of fileArray) {
    const ext = getExtension(file.name)
    if (isFileTypeAllowed(ext)) {
      allowedFiles.push(file)
    } else {
      rejectedNames.push(file.name)
    }
  }
  if (rejectedNames.length > 0) {
    message.warning(`以下文件类型不允许上传: ${rejectedNames.join(', ')}`)
  }
  if (allowedFiles.length === 0) {
    input.value = ''
    return
  }

  const current = props.uploadedFiles ?? []
  const newList = [...current]
  const tempIds: string[] = []

  // 立即将文件加入列表并显示（上传中状态）
  for (let i = 0; i < allowedFiles.length; i++) {
    const file = allowedFiles[i]
    if (!file) continue
    const tempId = `temp-${Date.now()}-${i}-${Math.random().toString(36).slice(2, 9)}`
    tempIds.push(tempId)
    newList.push({
      id: tempId,
      name: file.name,
      extension: getExtension(file.name),
      size: formatFileSize(file.size),
      uploading: true
    })
  }
  emit('update:uploadedFiles', newList)
  input.value = ''

  // 后台逐个上传，完成后更新对应项
  for (let i = 0; i < allowedFiles.length; i++) {
    const file = allowedFiles[i]
    const tempId = tempIds[i]
    if (!file || tempId === undefined) continue
    try {
      const res = await attachApi.upload(file)
      const data = res?.data?.data
      if (data) {
        const updated = (props.uploadedFiles ?? []).map((item) =>
          item.id === tempId ? { ...item, id: data, uploading: false } : item
        )
        emit('update:uploadedFiles', updated)
      } else {
        message.error(`上传失败: ${file.name}`)
        const filtered = (props.uploadedFiles ?? []).filter((f) => f.id !== tempId)
        emit('update:uploadedFiles', filtered)
      }
    } catch {
      message.error(`上传失败: ${file.name}`)
      const filtered = (props.uploadedFiles ?? []).filter((f) => f.id !== tempId)
      emit('update:uploadedFiles', filtered)
    }
  }
}
const removeFile = async (item: UploadedFileItem) => {
  // 上传中的文件无需调用删除接口
  if (!item.uploading && !item.id.startsWith('temp-')) {
    await attachApi.remove([item.id])
  }
  const newList = (props.uploadedFiles ?? []).filter((f) => f.id !== item.id)
  emit('update:uploadedFiles', newList)
}

const autoResize = () => {
  const el = editorRef.value
  if (!el) return
  el.style.height = 'auto'
  const maxHeight = 300
  el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`
}

/** HTML 转义 */
const escapeHtml = (str: string): string => {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/** 渲染标签为 HTML 字符串 */
const renderTagToHtml = (tagName: string, tagContent: string): string => {
  if (tagName === 'workspace-file') {
    const name = tagContent.split('/').pop() || tagContent
    return `<span contenteditable="false" class="editor-tag editor-tag-${tagName}" data-tag="${tagName}" data-content="${escapeHtml(tagContent)}"><span class="editor-tag-inner"><span class="editor-tag-name">${escapeHtml(name)}</span></span></span>`
  }
  // 未知标签，显示原始文本
  return escapeHtml(`<${tagName}>${tagContent}</${tagName}>`)
}

/** 将 modelValue 渲染到 contenteditable */
const renderEditorContent = (text: string) => {
  if (!editorRef.value) return
  editorRef.value.innerHTML = renderTaggedTextToHtml(text, renderTagToHtml)
}

/** 从 editor 提取内容并 emit */
const emitContentUpdate = () => {
  if (!editorRef.value) return
  const content = extractTextFromEditor(editorRef.value)
  if (content !== lastEmittedValue.value) {
    lastEmittedValue.value = content
    emit('update:modelValue', content)
  }
}

/** 检查并清理空的 editor 状态 */
const sanitizeEmptyEditor = () => {
  const editor = editorRef.value
  if (!editor) return

  // 获取所有文本节点的内容
  const textContent = editor.textContent || ''

  // 检查是否有标签元素（不可编辑的块）
  const hasTags = editor.querySelectorAll('[data-tag]').length > 0

  // 检查是否有实际内容（非空白字符）
  const hasTextContent = textContent.trim() !== ''

  // 判断是否真正为空
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
    textNodesToRemove.forEach(node => node.remove())
  }
}

/** 检测 @mention 触发 */
const checkMentionTrigger = () => {
  if (!props.workspacePanelOpen) {
    return;
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

/** 在 editor 中查找包含 @mention 触发符的文本节点（TreeWalker 方式，不依赖 selection） */
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

/** 插入 workspace-file 标签 */
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

/** 判断节点是否为标签元素 */
const isTagElement = (node: Node): node is HTMLElement => {
  return (
    node.nodeType === Node.ELEMENT_NODE &&
    (node as HTMLElement).hasAttribute('data-tag')
  )
}

/** 处理 editor input 事件 */
const handleEditorInput = () => {
  if (isComposing.value) return
  sanitizeEmptyEditor() // 清理空状态
  emitContentUpdate()
  nextTick(() => {
    checkMentionTrigger()
    autoResize()
  })
}

/** 处理 editor keydown 事件 */
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
        sanitizeEmptyEditor() // 清理空状态
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
            sanitizeEmptyEditor() // 清理空状态
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
        sanitizeEmptyEditor() // 清理空状态
        return
      }
    }
  }

  // 删除后检查并清理空状态
  nextTick(() => {
    sanitizeEmptyEditor()
  })
}

/** 处理粘贴，仅保留纯文本 - 彻底修复版 */
const handleEditorPaste = (e: ClipboardEvent) => {
  e.preventDefault()

  // 获取纯文本内容
  let text = e.clipboardData?.getData('text/plain') || ''

  // 如果粘贴的是 HTML，也提取纯文本
  if (!text) {
    const html = e.clipboardData?.getData('text/html')
    if (html) {
      // 创建临时 div 提取纯文本
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = html
      text = tempDiv.textContent || tempDiv.innerText || ''
    }
  }

  if (!text) return

  // 获取当前光标/选区
  const selection = window.getSelection()
  if (!selection || !selection.rangeCount) return

  const range = selection.getRangeAt(0)

  // 如果用户选中了文本，删除选中的内容
  if (!range.collapsed) {
    range.deleteContents()
  }

  // 创建文档片段来存储要粘贴的内容
  const fragment = document.createDocumentFragment()

  // 处理文本：将 \n 转换为 <br> 标签
  const lines = text.split('\n')

  lines.forEach((line, index) => {
    // 添加文本节点
    if (line) {
      fragment.appendChild(document.createTextNode(line))
    }

    // 如果不是最后一行，添加换行符
    if (index < lines.length - 1) {
      fragment.appendChild(document.createElement('br'))
    }
  })

  // 如果没有任何内容被创建，添加一个零宽空格
  if (fragment.childNodes.length === 0) {
    fragment.appendChild(document.createTextNode('\u200B')) // 零宽空格
  }

  // 将内容插入到光标位置
  range.insertNode(fragment)

  // 将光标移动到插入内容的末尾
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

  // 更新选区
  selection.removeAllRanges()
  selection.addRange(range)

  // 清理空状态并触发更新
  nextTick(() => {
    sanitizeEmptyEditor()
    emitContentUpdate()
    autoResize()
  })
}

/** 处理输入法组合开始 */
const handleCompositionStart = () => {
  isComposing.value = true
}

/** 处理输入法组合结束 */
const handleCompositionEnd = () => {
  isComposing.value = false
  emitContentUpdate()
  nextTick(() => {
    checkMentionTrigger()
    autoResize()
  })
}

/** 处理编辑器失去焦点 */
const handleEditorBlur = () => {
  mentionVisible.value = false
  // 失去焦点时也清理一下状态
  nextTick(() => {
    sanitizeEmptyEditor()
  })
}

/** 点击 @ 按钮触发 mention */
const handleMentionButtonClick = async () => {
  const editor = editorRef.value
  if (!editor) return

  // 聚焦编辑器
  editor.focus()

  // 等待 focus 完成
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

  // 光标移到插入文本之后
  range.setStartAfter(textNode)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)

  // 等待 DOM 更新完成
  await nextTick()

  // 触发内容更新
  emitContentUpdate()

  // 等待内容更新完成后再检测 @mention
  await nextTick()

  // 手动触发 mention 下拉显示
  checkMentionTrigger()

  // 如果下拉没有显示，强制显示（兜底方案）
  if (!mentionVisible.value && insertText.includes('@')) {
    mentionQuery.value = ''
    mentionVisible.value = true
    fetchFiles()
  }

  autoResize()
}

// 监听外部 modelValue 变化，重新渲染 editor
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal === lastEmittedValue.value) return
    renderEditorContent(newVal)
    lastEmittedValue.value = newVal
    nextTick(() => {
      autoResize()
      sanitizeEmptyEditor() // 清理空状态
    })
  },
  { immediate: true }
)
</script>

<template>
  <div class="chat-input-wrap">
    <input
      ref="fileInputRef"
      type="file"
      class="chat-file-input-hidden"
      :accept="fileAcceptAttr()"
      multiple
      @change="handleFileChange"
    />
    <!-- 已上传附件列表：显示于 editor 上方，支持横向滚动与单个移除 -->
    <div v-if="(uploadedFiles ?? []).length > 0" class="chat-input-files-row">
      <div class="chat-input-files-scroll">
        <div
          v-for="(item, index) in (uploadedFiles ?? [])"
          :key="item.id"
          @click="openPreview(item, index)"
          class="chat-input-file-item"
        >
          <span v-if="item.uploading" class="chat-input-file-loading">
            <LoadingOutlined spin />
          </span>
          <MediaIcon :type="(item.extension ?? getExtension(item.name)) || 'FILE'" size="19"/>
          <span class="chat-input-file-name" :title="item.name">{{ item.name }}</span>
          <button
            type="button"
            class="chat-input-file-remove"
            title="移除"
            @click.stop="removeFile(item)"
          >
            <CloseOutlined />
          </button>
        </div>
      </div>
    </div>

    <!-- 媒体预览组件 -->
    <MediaPreview
      v-model:visible="previewVisible"
      :items="uploadedFiles ?? []"
      :current-index="previewCurrentIndex"
    />

    <!-- contenteditable 编辑器区域（含 mention 下拉） -->
    <div class="chat-input-editor-wrapper">
      <div
        ref="editorRef"
        :data-placeholder="placeholder || '输入消息...'"
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
    <div class="chat-input-toolbar">
      <div class="chat-input-toolbar-left">
        <ATooltip placement="bottom">
          <template #title>
            <span v-if="enableMemory">{{ (memoryActive && enableMemory)?'点击关闭记忆':'点击开启记忆' }}</span>
            <span v-else>不支持记忆持久化</span>
          </template>
          <button
            :disabled="!enableMemory"
            type="button"
            class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
            :class="{ 'is-active': memoryActive && enableMemory }"
            @click="toggleMemory"
          >
            <ClockCircleOutlined />
          </button>
        </ATooltip>

        <ATooltip placement="bottom">
          <template #title>
            <span v-if="showToolProcess">{{ (toolProcessActive && showToolProcess)?'点击关闭工具调用历史':'点击显示工具调用历史' }}</span>
            <span v-else>不支持控制工具调用显示</span>
          </template>
          <button
            :disabled="!showToolProcess"
            type="button"
            class="chat-toolbar-btn chat-toolbar-btn-icon  chat-toolbar-btn-circle"
            :class="{ 'is-active': toolProcessActive && showToolProcess }"
            @click="toggleToolProcess"
          >
            <ThunderboltOutlined />
          </button>
        </ATooltip>
      </div>
      <div class="chat-input-toolbar-right">
        <!-- @ 添加上下文按钮 -->
        <ATooltip placement="bottom" title="添加上下文">
          <button
            :disabled="!workspacePanelOpen"
            type="button"
            class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
            @mousedown.prevent
            @click="handleMentionButtonClick"
          >
            @
          </button>
        </ATooltip>
        <ATooltip placement="bottom">
          <template #title>
            <span v-if="allowUploadFileType && allowUploadFileType?.length > 0">点击上传文件（{{allowUploadFileType?.join('、')}}）</span>
            <span v-else>不支持上传文件</span>
          </template>
          <button
            :disabled="!allowUploadFileType?.length"
            type="button"
            class="chat-toolbar-btn chat-toolbar-btn-icon chat-toolbar-btn-circle"
            style="margin-right: 15px"
            @click="handleFileClick"
          >
            <PaperClipOutlined />
          </button>
        </ATooltip>
        <button
          type="button"
          class="chat-send-btn-inner"
          :disabled="!isRunning && ((uploadedFiles ?? []).some((f) => f.uploading) || (!modelValue.trim() && (uploadedFiles ?? []).filter((f) => !f.uploading).length === 0))"
          @click="isRunning ? $emit('abort') : $emit('send')"
        >
          <template v-if="isRunning"><div class="send"></div></template>
          <ArrowUpOutlined v-else />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.chat-input-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  border-radius: 24px;
  border: 1px solid var(--color-border-light);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 10px 12px;
  background-color: $chat-bg-main;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
  max-height: 400px;

  &:focus-within {
    border-color: $chat-primary;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), 0 0 0 2px rgba($chat-primary, 0.1);
  }
}

.chat-input-files-row {
  flex-shrink: 0;
  min-height: 0;
  overflow: hidden;
}

.chat-input-files-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 0;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border-light) transparent;

  &::-webkit-scrollbar {
    height: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--color-border-light);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: var(--color-text-placeholder);
  }
}

.chat-input-file-item {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 280px;
  padding: 6px 10px;
  background: #F5F7FA;
  border-radius: var(--border-radius-md);
  font-size: var(--font-size-sm);
  transition: background-color 0.2s ease, border-color 0.2s ease;
  cursor: pointer;

  &:hover {
    background: rgba($chat-primary, 0.1);
  }
}

.chat-input-file-loading {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  color: $chat-primary;
  font-size: 14px;
}

.chat-input-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
}

.chat-input-file-remove {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-placeholder);
  cursor: pointer;
  border-radius: 50%;
  font-size: 12px;
  transition: color 0.2s ease, background-color 0.2s ease;

  &:hover {
    color: var(--color-text-primary);
    background: rgba(0, 0, 0, 0.08);
  }
}

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

.chat-input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  min-height: 36px;
}

.chat-input-toolbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-input-toolbar-right {
  display: flex;
  align-items: center;
}

.chat-toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background-color: #f5f5f5;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color 0.2s ease, background-color 0.2s ease;
  border-radius: var(--border-radius-md);
  margin-right: 10px;

  &:hover {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.06);
  }

  &.is-active {
    color: $chat-primary;
    background-color: rgba($chat-primary, 0.1);
    font-weight: 500;
  }

  &:disabled,
  &[disabled] {
    &:hover {
      cursor: not-allowed;
      color: var(--color-text-secondary);
      background-color: transparent;
    }
  }
}

.chat-toolbar-btn-text {
  padding: 6px 10px;
  font-size: var(--font-size-sm);
}

.chat-toolbar-btn-icon {
  width: 32px;
  height: 32px;
  font-size: 16px;
}

.chat-toolbar-btn-circle {
  border-radius: 50%;
}

.chat-file-input-hidden {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.chat-send-btn-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: $chat-primary;
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    transform: scale(1.05);
  }

  &:disabled {
    background-color: #e0e0e0;
    cursor: not-allowed;
    opacity: 0.6;
  }

  .send {
    width: 13px;
    height: 13px;
    background-color: #fff;
    border-radius: 2px;
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
