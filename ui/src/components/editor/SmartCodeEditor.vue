<template>
  <div ref="fullscreenElement" class="smart-code-editor" :class="[themeClass, { 'readonly-mode': readonly }]">
    <!-- 工具栏 -->
    <div v-if="showToolbar" class="editor-toolbar">
      <div class="toolbar-left">
        <select v-if="showChangeLanguage" v-model="currentLanguage" @change="handleLanguageChange" class="language-select">
          <option v-for="lang in supportedLanguages" :key="lang.value" :value="lang.value">
            {{ lang.label }}
          </option>
        </select>

        <button
          v-if="showThemeToggle"
          @click="toggleTheme"
          class="toolbar-btn"
          :title="currentTheme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
        >
          <BulbFilled v-if="currentTheme === 'dark'" class="btn-icon" />
          <BulbOutlined v-else class="btn-icon" />
        </button>

        <button
          v-if="showFormatButton && isFormattable"
          @click="formatCode"
          class="toolbar-btn"
          title="格式化代码"
        >
          <FormatPainterOutlined class="btn-icon" />
        </button>

        <button
          v-if="showCopyButton"
          @click="copyToClipboard"
          class="toolbar-btn"
          title="复制代码"
        >
          <CopyOutlined class="btn-icon" />
        </button>
        <button
          v-if="showFullscreen"
          @click="handleToggleFullscreen"
          class="toolbar-btn"
          :title="isFullscreen ? '退出全屏':'进入全屏'"
        >
          <FullscreenExitOutlined v-if="isFullscreen" class="btn-icon"/>
          <FullscreenOutlined v-else class="btn-icon"/>
        </button>
      </div>

      <div class="toolbar-right">
        <div v-if="showStatusBar" class="status-info">
          <span class="char-count">字符: {{ charCount }}</span>
          <span class="line-count">行数: {{ lineCount }}</span>
        </div>
      </div>
    </div>

    <!-- 编辑器容器 -->
    <div
      ref="editorContainer"
      class="editor-container"
      :style="containerStyle"
    ></div>

    <!-- 加载状态 - 使用骨架屏减少闪烁 -->
    <div v-if="loading && !editorReady" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载编辑器中...</span>
    </div>

    <!-- 错误提示 - 使用虚拟列表优化大量错误显示 -->
    <div v-if="errors.length > 0 && showErrorPanel" class="error-panel">
      <div class="error-list" ref="errorListRef" @scroll="handleErrorScroll">
        <div
          v-for="(error, index) in visibleErrors"
          :key="index"
          class="error-item"
          @click="jumpToError(error)"
        >
          <span class="error-line">第{{ error.line }}行:</span>
          <span class="error-message">{{ error.message }}</span>
        </div>
        <div v-if="errors.length > visibleErrors.length" class="error-more">
          还有 {{ errors.length - visibleErrors.length }} 个错误...
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  onMounted,
  onUnmounted,
  watch,
  computed,
  nextTick,
  shallowRef,
  markRaw
} from 'vue'
import {
  BulbOutlined,
  BulbFilled,
  FormatPainterOutlined,
  CopyOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useFullscreen } from '@/composables/useFullscreen'
import { debounce } from 'lodash-es'

// 定义组件属性
interface Props {
  modelValue: string
  language?: 'java' | 'txt' | 'html' | 'javascript' | 'css' | 'xml' | 'json' | 'markdown'
  theme?: 'light' | 'dark'
  readonly?: boolean
  height?: string
  width?: string
  showLineNumbers?: boolean
  showActiveLineHighlight?: boolean
  showToolbar?: boolean
  showChangeLanguage?: boolean
  showStatusBar?: boolean
  showErrorPanel?: boolean
  showFormatButton?: boolean
  showCopyButton?: boolean
  showFullscreen?: boolean
  showThemeToggle?: boolean
  lineWrapping?: boolean
  tabSize?: number
  placeholder?: string
  autofocus?: boolean
  lazyLoad?: boolean // 是否延迟加载
  errorVirtualizationThreshold?: number // 错误虚拟化阈值
  // 新增：是否严格验证 JSON
  strictJsonValidation?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'update:theme', theme: 'light' | 'dark'): void
  (e: 'change', value: string): void
  (e: 'focus'): void
  (e: 'blur'): void
  (e: 'save'): void
  (e: 'language-change', language: string): void
  (e: 'error', errors: Array<{line: number, message: string}>): void
}

const props = withDefaults(defineProps<Props>(), {
  language: 'javascript',
  theme: 'dark',
  readonly: false,
  height: '400px',
  width: '100%',
  showLineNumbers: true,
  showActiveLineHighlight: true,
  showToolbar: true,
  showChangeLanguage: false,
  showStatusBar: true,
  showErrorPanel: true,
  showFormatButton: true,
  showCopyButton: true,
  showFullscreen: true,
  showThemeToggle: false,
  lineWrapping: true,
  tabSize: 2,
  placeholder: '',
  autofocus: false,
  lazyLoad: false,
  errorVirtualizationThreshold: 50,
  strictJsonValidation: true // 默认开启严格验证
})

const emit = defineEmits<Emits>()

// 响应式数据
const fullscreenElement = ref<HTMLElement | null>(null)
const editorContainer = ref<HTMLElement>()
const errorListRef = ref<HTMLElement>()
const editorView = shallowRef<any>(null)
const editorReady = ref(false)
const currentLanguage = ref(props.language)
const currentTheme = ref(props.theme)
const loading = ref(true)
const charCount = ref(0)
const lineCount = ref(0)
const errors = ref<Array<{line: number, message: string}>>([])
const visibleErrors = ref<Array<{line: number, message: string}>>([])
const isInternalUpdate = ref(false) // 新增：标记是否是内部更新

// 缓存 CodeMirror 模块
const codemirrorModules = shallowRef<any>({})
const languageExtensions = shallowRef<Map<string, any>>(new Map())
const themeExtensions = shallowRef<Map<string, any>>(new Map())

const {
  isFullscreen,
  toggleFullscreen,
  isSupported
} = useFullscreen(fullscreenElement)

// 计算属性
const containerStyle = computed(() => ({
  height: isFullscreen.value ? 'calc(100vh - 41px)' : props.height,
  width: props.width
}))

const themeClass = computed(() => `theme-${currentTheme.value}`)

const isFormattable = computed(() => {
  return ['json', 'xml', 'javascript', 'css', 'html', 'markdown'].includes(currentLanguage.value)
})

const supportedLanguages = [
  { value: 'javascript', label: 'JavaScript' },
  { value: 'java', label: 'Java' },
  { value: 'html', label: 'HTML' },
  { value: 'css', label: 'CSS' },
  { value: 'xml', label: 'XML' },
  { value: 'json', label: 'JSON' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'txt', label: 'Text' }
]

// 初始化 CodeMirror 模块
const initCodeMirrorModules = async () => {
  if (Object.keys(codemirrorModules.value).length > 0) return

  try {
    const [
      stateModule,
      viewModule,
      commandsModule,
      languageModule,
      autocompleteModule,
      searchModule,
      themeModule
    ] = await Promise.all([
      import('@codemirror/state'),
      import('@codemirror/view'),
      import('@codemirror/commands'),
      import('@codemirror/language'),
      import('@codemirror/autocomplete'),
      import('@codemirror/search'),
      import('@codemirror/theme-one-dark')
    ])

    codemirrorModules.value = markRaw({
      EditorState: stateModule.EditorState,
      EditorView: viewModule.EditorView,
      keymap: viewModule.keymap,
      lineNumbers: viewModule.lineNumbers,
      highlightActiveLine: viewModule.highlightActiveLine,
      highlightActiveLineGutter: viewModule.highlightActiveLineGutter,
      drawSelection: viewModule.drawSelection,
      dropCursor: viewModule.dropCursor,
      rectangularSelection: viewModule.rectangularSelection,
      crosshairCursor: viewModule.crosshairCursor,
      highlightSpecialChars: viewModule.highlightSpecialChars,
      defaultKeymap: commandsModule.defaultKeymap,
      indentWithTab: commandsModule.indentWithTab,
      history: commandsModule.history,
      historyKeymap: commandsModule.historyKeymap,
      bracketMatching: languageModule.bracketMatching,
      foldGutter: languageModule.foldGutter,
      indentOnInput: languageModule.indentOnInput,
      syntaxHighlighting: languageModule.syntaxHighlighting,
      defaultHighlightStyle: languageModule.defaultHighlightStyle,
      closeBrackets: autocompleteModule.closeBrackets,
      closeBracketsKeymap: autocompleteModule.closeBracketsKeymap,
      searchKeymap: searchModule.searchKeymap,
      highlightSelectionMatches: searchModule.highlightSelectionMatches,
      oneDark: themeModule.oneDark
    })

    loading.value = false
  } catch (error) {
    console.error('Failed to load CodeMirror:', error)
    loading.value = false
  }
}

// 缓存语言扩展
const getCachedLanguageExtension = async (lang: string) => {
  if (languageExtensions.value.has(lang)) {
    return languageExtensions.value.get(lang)
  }

  try {
    let extension = null
    switch (lang) {
      case 'javascript':
        const { javascript } = await import('@codemirror/lang-javascript')
        extension = javascript()
        break
      case 'java':
        const { java } = await import('@codemirror/lang-java')
        extension = java()
        break
      case 'html':
        const { html } = await import('@codemirror/lang-html')
        extension = html()
        break
      case 'css':
        const { css } = await import('@codemirror/lang-css')
        extension = css()
        break
      case 'xml':
        const { xml } = await import('@codemirror/lang-xml')
        extension = xml()
        break
      case 'json':
        const { json } = await import('@codemirror/lang-json')
        // 修复：为 JSON 语言添加更宽松的解析配置
        extension = json({
          allowComments: true,
          allowTrailingComma: true
        })
        break
      case 'markdown':
        const { markdown } = await import('@codemirror/lang-markdown')
        extension = markdown()
        break
      default:
        extension = []
    }

    languageExtensions.value.set(lang, markRaw(extension))
    return extension
  } catch (error) {
    console.error('Failed to load language extension:', error)
    return []
  }
}

// 缓存主题扩展
const getCachedThemeExtension = (theme: string) => {
  if (themeExtensions.value.has(theme)) {
    return themeExtensions.value.get(theme)
  }

  const { EditorView } = codemirrorModules.value
  if (!EditorView) return null

  const extension = theme === 'dark'
    ? codemirrorModules.value.oneDark
    : EditorView.theme({
      "&": {
        color: "#333",
        backgroundColor: "#fff",
      },
      ".cm-content": {
        caretColor: "#333",
      },
      ".cm-cursor, .cm-dropCursor": {
        borderLeftColor: "#333",
      },
      "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection": {
        backgroundColor: "#d1e7ff",
      },
      ".cm-gutters": {
        backgroundColor: "#f5f5f5",
        color: "#666",
        borderRight: "1px solid #ddd",
      },
      ".cm-activeLineGutter": {
        backgroundColor: "#e8e8e8",
      },
      ".cm-activeLine": {
        backgroundColor: "#f7f7f7",
      },
    })

  themeExtensions.value.set(theme, markRaw(extension))
  return extension
}

// 创建编辑器配置
const createEditorConfig = async (content: string, selection?: any) => {
  const modules = codemirrorModules.value
  const { EditorState, EditorView } = modules

  const languageExtension = await getCachedLanguageExtension(currentLanguage.value)
  const themeExtension = getCachedThemeExtension(currentTheme.value)

  const extensions = [
    modules.history(),
    modules.drawSelection(),
    modules.dropCursor(),
    EditorState.allowMultipleSelections.of(true),
    modules.indentOnInput(),
    modules.syntaxHighlighting(modules.defaultHighlightStyle, { fallback: true }),
    themeExtension,
    props.showLineNumbers ? modules.lineNumbers() : [],
    EditorState.readOnly.of(props.readonly),
    EditorState.tabSize.of(props.tabSize),
  ]

  if (languageExtension) {
    extensions.push(languageExtension)
  }

  if (props.showActiveLineHighlight) {
    extensions.push(
      modules.highlightActiveLine(),
      modules.highlightActiveLineGutter()
    )
  }

  if (props.lineWrapping) {
    extensions.push(EditorView.lineWrapping)
  }

  extensions.push(
    modules.highlightSpecialChars(),
    modules.rectangularSelection(),
    modules.crosshairCursor(),
    modules.closeBrackets(),
    modules.bracketMatching(),
    modules.highlightSelectionMatches()
  )

  const keymaps = [
    ...modules.defaultKeymap,
    ...modules.historyKeymap,
    ...modules.closeBracketsKeymap,
    ...modules.searchKeymap,
    modules.indentWithTab,
    {
      key: 'Mod-s',
      run: () => {
        emit('save')
        return true
      },
      preventDefault: true
    }
  ]

  extensions.push(modules.keymap.of(keymaps))
  extensions.push(createUpdateListener())

  const config: any = {
    doc: content,
    extensions: extensions.filter(ext => ext != null && (Array.isArray(ext) ? ext.length > 0 : true))
  }

  if (selection) {
    config.selection = selection
  }

  return EditorState.create(config)
}

// 创建更新监听器
const createUpdateListener = () => {
  const { EditorView } = codemirrorModules.value
  return EditorView.updateListener.of((update: any) => {
    // 立即处理文档变化
    if (update.docChanged) {
      const content = update.state.doc.toString()

      // 标记为内部更新
      isInternalUpdate.value = true

      // 更新统计信息
      updateStatistics(update.state)

      // 触发 v-model 更新
      emit('update:modelValue', content)
      emit('change', content)

      // 异步验证代码，不阻塞更新
      setTimeout(() => {
        validateCode(content)
      }, 0)

      // 下一帧重置内部更新标记
      nextTick(() => {
        isInternalUpdate.value = false
      })
    }
  })
}

// 更新统计信息
const updateStatistics = (state: any) => {
  charCount.value = state.doc.length
  lineCount.value = state.doc.lines
}

// 验证代码（防抖）
const validateCode = debounce((code: string) => {
  // 清除之前的错误
  const oldErrors = [...errors.value]
  errors.value = []

  // 只在非只读模式下且需要验证时进行验证
  if (props.readonly) return

  try {
    // 根据语言进行验证
    switch (currentLanguage.value) {
      case 'json':
        if (code.trim() && props.strictJsonValidation) {
          JSON.parse(code)
        }
        break
      case 'javascript':
        if (code.trim() && props.strictJsonValidation) {
          new Function(code)
        }
        break
    }
  } catch (error: any) {
    // 解析错误行号
    let line = 1
    let message = error.message

    // 尝试提取行号
    const lineMatch = error.message.match(/at position (\d+)/)
    if (lineMatch) {
      const position = parseInt(lineMatch[1])
      line = code.substring(0, position).split('\n').length
      message = `JSON 解析错误: ${message}`
    } else {
      const lineMatch2 = error.message.match(/line (\d+)/)
      if (lineMatch2) {
        line = parseInt(lineMatch2[1])
      }
    }

    errors.value.push({
      line,
      message: message.split('\n')[0]
    })

    emit('error', errors.value)

    // 更新可见错误
    updateVisibleErrors()
  }
}, 300)

// 错误虚拟化
const updateVisibleErrors = () => {
  if (errors.value.length <= props.errorVirtualizationThreshold) {
    visibleErrors.value = errors.value
    return
  }

  const scrollTop = errorListRef.value?.scrollTop || 0
  const itemHeight = 24
  const containerHeight = errorListRef.value?.clientHeight || 120
  const startIndex = Math.floor(scrollTop / itemHeight)
  const endIndex = Math.min(
    startIndex + Math.ceil(containerHeight / itemHeight) + 2,
    errors.value.length
  )

  visibleErrors.value = errors.value.slice(startIndex, endIndex)
}

const handleErrorScroll = () => {
  updateVisibleErrors()
}

// 初始化编辑器
const initEditor = async () => {
  if (!editorContainer.value || editorReady.value) return

  try {
    await initCodeMirrorModules()

    const { EditorView } = codemirrorModules.value
    const state = await createEditorConfig(props.modelValue)

    editorView.value = markRaw(new EditorView({
      state,
      parent: editorContainer.value
    }))

    // 添加事件监听
    const focusHandler = () => emit('focus')
    const blurHandler = () => emit('blur')

    editorView.value.dom.addEventListener('focus', focusHandler)
    editorView.value.dom.addEventListener('blur', blurHandler)

    // 存储处理器以便清理
    editorView.value._handlers = { focusHandler, blurHandler }

    updateStatistics(state)

    // 首次验证
    validateCode(props.modelValue)

    editorReady.value = true

    if (props.autofocus) {
      setTimeout(() => {
        editorView.value?.focus()
      }, 100)
    }
  } catch (error) {
    console.error('Failed to initialize editor:', error)
  }
}

// 切换语言
const handleLanguageChange = async () => {
  if (!editorView.value || !codemirrorModules.value.EditorState) return

  const content = editorView.value.state.doc.toString()
  const selection = editorView.value.state.selection

  const newState = await createEditorConfig(content, selection)
  editorView.value.setState(newState)

  emit('language-change', currentLanguage.value)

  // 重新验证
  validateCode(content)
}

// 切换主题
const toggleTheme = () => {
  currentTheme.value = currentTheme.value === 'dark' ? 'light' : 'dark'
  emit('update:theme', currentTheme.value)
}

// 格式化代码
const formatCode = () => {
  if (!editorView.value || !isFormattable.value) return

  const code = editorView.value.state.doc.toString()
  let formatted = code

  try {
    switch (currentLanguage.value) {
      case 'json':
        if (code.trim()) {
          try {
            // 尝试解析并重新格式化 JSON
            const parsed = JSON.parse(code)
            formatted = JSON.stringify(parsed, null, props.tabSize)
          } catch (e) {
            // 如果解析失败，保留原内容并显示错误
            message.warning('无法格式化无效的 JSON')
            return
          }
        }
        break
      case 'javascript':
        formatted = code
          .replace(/(\w)\{/g, '$1 {')
          .replace(/\)\{/g, ') {')
          .replace(/(if|for|while|catch|switch)\(/g, '$1 (')
        break
      case 'html':
      case 'xml':
        formatted = code.replace(/>\s*</g, '>\n<')
        break
      case 'css':
        formatted = code
          .replace(/\}\s*/g, '}\n')
          .replace(/\{\s*/g, '{\n  ')
          .replace(/;\s*/g, ';\n  ')
        break
      case 'markdown':
        formatted = code
          .replace(/\r\n/g, '\n')
          .replace(/\n{3,}/g, '\n\n')
          .trim()
        break
    }

    if (formatted !== code) {
      editorView.value.dispatch({
        changes: {
          from: 0,
          to: editorView.value.state.doc.length,
          insert: formatted
        }
      })
      message.success('格式化成功')
    }
  } catch (error) {
    console.error('格式化失败:', error)
    message.error('格式化失败')
  }
}

// 复制到剪贴板
const copyToClipboard = async () => {
  if (!editorView.value) return

  const code = editorView.value.state.doc.toString()
  try {
    await navigator.clipboard.writeText(code)
    message.success('复制成功')
  } catch (err) {
    console.error('复制失败:', err)
    message.error('复制失败')
  }
}

const handleToggleFullscreen = async (): Promise<void> => {
  if (!isSupported) {
    message.warning('不支持此功能')
    return
  }

  try {
    await toggleFullscreen(fullscreenElement.value)
    nextTick(() => {
      editorView.value?.requestMeasure()
    })
  } catch (error) {
    console.error('切换失败:', error)
  }
}

const jumpToError = (error: {line: number, message: string}) => {
  if (!editorView.value) return

  const line = editorView.value.state.doc.line(error.line)
  editorView.value.dispatch({
    selection: { anchor: line.from },
    effects: codemirrorModules.value.EditorView?.scrollIntoView(line.from)
  })
  editorView.value.focus()
}

// 监听属性变化
watch(() => props.modelValue, (newValue) => {
  // 如果编辑器未准备好，忽略
  if (!editorView.value || !editorReady.value) return

  // 如果是内部更新导致的，忽略
  if (isInternalUpdate.value) return

  const currentValue = editorView.value.state.doc.toString()

  // 只有当内容真正不同时才更新
  if (newValue !== currentValue) {
    try {
      editorView.value.dispatch({
        changes: {
          from: 0,
          to: editorView.value.state.doc.length,
          insert: newValue
        }
      })

      // 触发验证
      validateCode(newValue)
    } catch (error) {
      console.error('更新编辑器内容失败:', error)
    }
  }
})

watch(() => currentTheme.value, async () => {
  if (!editorView.value || !editorReady.value) return

  const content = editorView.value.state.doc.toString()
  const selection = editorView.value.state.selection

  const newState = await createEditorConfig(content, selection)
  editorView.value.setState(newState)
})

watch(() => props.theme, (newTheme) => {
  if (newTheme !== currentTheme.value) {
    currentTheme.value = newTheme
  }
})

watch(() => props.readonly, (readonly) => {
  if (editorView.value && codemirrorModules.value.EditorState) {
    editorView.value.dispatch({
      effects: codemirrorModules.value.EditorState.readOnly.of(readonly)
    })
  }
})

watch(() => currentLanguage.value, () => {
  // 语言变化后重新验证
  if (editorView.value) {
    const content = editorView.value.state.doc.toString()
    validateCode(content)
  }
})

// 生命周期
onMounted(() => {
  if (props.lazyLoad) {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          initEditor()
          observer.disconnect()
        }
      },
      { threshold: 0.1 }
    )

    if (editorContainer.value) {
      observer.observe(editorContainer.value)
    }
  } else {
    initEditor()
  }
})

onUnmounted(() => {
  if (editorView.value) {
    if (editorView.value._handlers) {
      editorView.value.dom.removeEventListener('focus', editorView.value._handlers.focusHandler)
      editorView.value.dom.removeEventListener('blur', editorView.value._handlers.blurHandler)
    }
    editorView.value.destroy()
    editorView.value = null
  }

  validateCode.cancel()
})

// 暴露方法
defineExpose({
  getEditor: () => editorView.value,
  focus: () => editorView.value?.focus(),
  blur: () => editorView.value?.contentDOM?.blur(),
  formatCode,
  copyToClipboard,
  getContent: () => editorView.value?.state.doc.toString() || '',
  setContent: (content: string) => {
    if (editorView.value) {
      isInternalUpdate.value = true
      editorView.value.dispatch({
        changes: {
          from: 0,
          to: editorView.value.state.doc.length,
          insert: content
        }
      })
      nextTick(() => {
        isInternalUpdate.value = false
      })
      validateCode(content)
    }
  },
  insertText: (text: string, position?: number) => {
    if (editorView.value) {
      isInternalUpdate.value = true
      const pos = position ?? editorView.value.state.selection.main.head
      editorView.value.dispatch({
        changes: {
          from: pos,
          to: pos,
          insert: text
        },
        selection: { anchor: pos + text.length }
      })
      nextTick(() => {
        isInternalUpdate.value = false
      })
    }
  },
  replaceSelection: (text: string) => {
    if (editorView.value) {
      isInternalUpdate.value = true
      const selection = editorView.value.state.selection.main
      editorView.value.dispatch({
        changes: {
          from: selection.from,
          to: selection.to,
          insert: text
        },
        selection: { anchor: selection.from + text.length }
      })
      nextTick(() => {
        isInternalUpdate.value = false
      })
    }
  },
  getSelection: () => {
    if (!editorView.value) return ''
    const selection = editorView.value.state.selection.main
    return editorView.value.state.doc.sliceString(selection.from, selection.to)
  },
  clearErrors: () => {
    errors.value = []
  },
  refresh: () => {
    editorView.value?.requestMeasure()
  }
})
</script>

<style scoped>
.smart-code-editor {
  font-family: 'AlimamaFangYuan', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
}

/* CodeMirror 编辑器字体设置 */
.smart-code-editor :deep(.cm-editor),
.smart-code-editor :deep(.cm-content),
.smart-code-editor :deep(.cm-line) {
  font-family: 'AlimamaFangYuan', 'Consolas', 'Monaco', 'Courier New', monospace;
  letter-spacing: 1px;
  line-height: 1.8;
  font-weight: 500;
  font-size: 14px;
}

.smart-code-editor :deep(.cm-content) {
  padding: 8px 0;
}

/* 行号样式 */
.smart-code-editor :deep(.cm-gutters) {
  min-width: 50px;
  white-space: nowrap;
}

.smart-code-editor :deep(.cm-lineNumbers) {
  min-width: 40px;
  white-space: nowrap;
  text-align: right;
  padding-right: 8px;
}

.smart-code-editor :deep(.cm-lineNumbers .cm-gutterElement) {
  min-width: 40px;
  white-space: nowrap;
  text-align: right;
}

.smart-code-editor.theme-dark {
  background: #1e1e1e;
  color: #d4d4d4;
}

.smart-code-editor.theme-light {
  background: #ffffff;
  color: #333333;
  border: 1px solid #e0e0e0;
}

/* 工具栏样式 */
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid;
}

.smart-code-editor.theme-dark .editor-toolbar {
  background: #252526;
  border-bottom-color: #3c3c3c;
}

.smart-code-editor.theme-light .editor-toolbar {
  background: #f5f5f5;
  border-bottom-color: #e0e0e0;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.language-select {
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid;
  background: inherit;
  color: inherit;
  font-size: 14px;
  min-width: 120px;
  cursor: pointer;
}

.smart-code-editor.theme-dark .language-select {
  border-color: #3c3c3c;
  background: #3c3c3c;
}

.smart-code-editor.theme-light .language-select {
  border-color: #d0d0d0;
  background: #ffffff;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid;
  background: inherit;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.toolbar-btn:hover {
  opacity: 0.8;
}

.smart-code-editor.theme-dark .toolbar-btn {
  border-color: #3c3c3c;
  background: #3c3c3c;
}

.smart-code-editor.theme-light .toolbar-btn {
  border-color: #d0d0d0;
  background: #ffffff;
}

.btn-icon {
  font-size: 14px;
  display: inline-flex;
  align-items: center;
}

/* 状态栏 */
.status-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  opacity: 0.8;
}

/* 编辑器容器 */
.editor-container {
  overflow: auto;
  min-height: 100px;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
}

.editor-container :deep(.cm-scroller) {
  overflow: auto;
}

/* 滚动条样式 */
.smart-code-editor.theme-dark .editor-container::-webkit-scrollbar,
.smart-code-editor.theme-dark .editor-container :deep(.cm-scroller)::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.smart-code-editor.theme-dark .editor-container::-webkit-scrollbar-track,
.smart-code-editor.theme-dark .editor-container :deep(.cm-scroller)::-webkit-scrollbar-track {
  background: #1e1e1e;
  border-radius: 5px;
}

.smart-code-editor.theme-dark .editor-container::-webkit-scrollbar-thumb,
.smart-code-editor.theme-dark .editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb {
  background: #424242;
  border-radius: 5px;
  border: 2px solid #1e1e1e;
}

.smart-code-editor.theme-dark .editor-container::-webkit-scrollbar-thumb:hover,
.smart-code-editor.theme-dark .editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb:hover {
  background: #4e4e4e;
}

.smart-code-editor.theme-light .editor-container::-webkit-scrollbar,
.smart-code-editor.theme-light .editor-container :deep(.cm-scroller)::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.smart-code-editor.theme-light .editor-container::-webkit-scrollbar-track,
.smart-code-editor.theme-light .editor-container :deep(.cm-scroller)::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 5px;
}

.smart-code-editor.theme-light .editor-container::-webkit-scrollbar-thumb,
.smart-code-editor.theme-light .editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 5px;
  border: 2px solid #f5f5f5;
}

.smart-code-editor.theme-light .editor-container::-webkit-scrollbar-thumb:hover,
.smart-code-editor.theme-light .editor-container :deep(.cm-scroller)::-webkit-scrollbar-thumb:hover {
  background: #b0b0b0;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.smart-code-editor.theme-dark .loading-overlay {
  background: rgba(30, 30, 30, 0.8);
  color: #d4d4d4;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误面板 */
.error-panel {
  border-top: 1px solid;
  background: inherit;
  max-height: 120px;
  overflow-y: auto;
}

.smart-code-editor.theme-dark .error-panel {
  border-top-color: #f44336;
  background: #2a0f0f;
}

.smart-code-editor.theme-light .error-panel {
  border-top-color: #ffcdd2;
  background: #ffebee;
}

.error-list {
  padding: 0 12px;
}

.error-item {
  padding: 0;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  gap: 8px;
  height: 24px;
  line-height: 24px;
}

.error-item:hover {
  opacity: 0.8;
}

.error-line {
  font-weight: bold;
  min-width: 60px;
}

.smart-code-editor.theme-dark .error-line {
  color: #ff9800;
}

.smart-code-editor.theme-light .error-line {
  color: #f57c00;
}

.error-message {
  flex: 1;
  word-break: break-word;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.error-more {
  padding: 4px 0;
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* 只读模式 */
.readonly-mode :deep(.cm-content) {
  opacity: 0.8;
}

.readonly-mode .editor-toolbar {
  opacity: 0.7;
}
</style>
