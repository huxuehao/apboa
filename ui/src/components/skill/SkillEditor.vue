<script setup lang="ts">
/**
 * 技能文件编辑器组件
 * 嵌入 SmartCodeEditor，提供保存状态管理和文件切换保护
 */
import { ref, watch, computed } from 'vue'
import { Modal, Button } from 'ant-design-vue'
import {
  SaveOutlined,
  RollbackOutlined,
  CheckCircleFilled,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import type { SkillFileTreeNode } from '@/types'
import * as skillApi from '@/api/skill'

const props = defineProps<{
  skillId: string
  file: SkillFileTreeNode | null
}>()

const emit = defineEmits<{
  (e: 'dirty-change', dirty: boolean): void
  (e: 'saved'): void
}>()

// 当前编辑内容
const editorContent = ref('')
// 原始内容（用于比较 dirty）
const originalContent = ref('')
// 保存状态：idle | saving | saved
const saveStatus = ref<'idle' | 'saving' | 'saved'>('saved')
// 内容是否已加载
const loaded = ref(false)
// 保存状态计时器
let savedTimer: ReturnType<typeof setTimeout> | null = null

// 是否有未保存的修改
const isDirty = computed(() => editorContent.value !== originalContent.value)

// 当前文件路径面包屑
const breadcrumb = computed(() => {
  if (!props.file) return ''
  return props.file.path.split('/').join(' / ')
})

// 监听文件切换
watch(
  () => props.file,
  async (newFile, oldFile) => {
    // 切换前检查未保存
    if (oldFile && isDirty.value && props.file?.fileId) {
      const confirmed = await showUnsavedConfirm()
      if (!confirmed) {
        return
      }
    }

    clearSavedTimer()

    if (newFile && !newFile.directory) {
      await loadFileContent(newFile)
    } else {
      editorContent.value = ''
      originalContent.value = ''
      loaded.value = false
      saveStatus.value = 'saved'
    }
  },
)

/**
 * 加载文件内容
 */
async function loadFileContent(node: SkillFileTreeNode) {
  loaded.value = false
  try {
    if (node.fileId) {
      if (node.content !== undefined) {
        editorContent.value = node.content || ''
      } else {
        const res = await skillApi.getFileContent(props.skillId, node.path)
        editorContent.value = res.data?.data || ''
      }
    } else {
      const res = await skillApi.getFileContent(props.skillId, node.path)
      editorContent.value = res.data?.data || ''
    }
    originalContent.value = editorContent.value
    saveStatus.value = 'saved'
  } catch {
    editorContent.value = ''
    originalContent.value = ''
    saveStatus.value = 'saved'
  } finally {
    loaded.value = true
  }
}

/**
 * 处理编辑器内容变化
 */
function handleContentChange(newContent: string) {
  editorContent.value = newContent
  if (isDirty.value) {
    clearSavedTimer()
    if (saveStatus.value !== 'idle') {
      saveStatus.value = 'idle'
      emit('dirty-change', true)
    }
  }
}

/**
 * 保存文件
 */
async function handleSave() {
  if (!props.file || saveStatus.value === 'saving') return

  saveStatus.value = 'saving'
  try {
    const content = editorContent.value
    if (props.file.fileId) {
      await skillApi.updateFile(props.file.fileId, content)
    } else {
      await skillApi.writeFileSystemFile(props.skillId, {
        path: props.file.path,
        content,
      })
    }
    originalContent.value = content
    saveStatus.value = 'saved'
    emit('dirty-change', false)
    emit('saved')

    savedTimer = setTimeout(() => {
      saveStatus.value = 'idle'
    }, 3000)
  } catch {
    saveStatus.value = 'idle'
  }
}

/**
 * 放弃更改，恢复原始内容
 */
function handleRevert() {
  editorContent.value = originalContent.value
  saveStatus.value = 'saved'
  emit('dirty-change', false)
}

/**
 * 显示未保存确认弹窗
 */
function showUnsavedConfirm(): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: '未保存的更改',
      content: '当前文件有未保存的修改，是否放弃更改并切换文件？',
      okText: '放弃更改',
      cancelText: '继续编辑',
      okButtonProps: { danger: true },
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function clearSavedTimer() {
  if (savedTimer) {
    clearTimeout(savedTimer)
    savedTimer = null
  }
}

/**
 * 暴露方法给父组件
 */
defineExpose({
  isDirty: () => isDirty.value,
  save: handleSave,
})

/**
 * 处理 Ctrl+S 保存
 */
function handleEditorSave() {
  handleSave()
}
</script>

<template>
  <div class="skill-editor">
    <!-- 空状态 -->
    <div v-if="!file" class="editor-empty">
      <div class="empty-text">请在左侧选择文件进行编辑</div>
    </div>

    <template v-else-if="loaded">
      <!-- 顶部工具栏 -->
      <div class="editor-toolbar">
        <span class="breadcrumb">{{ breadcrumb }}</span>
        <div class="toolbar-actions">
          <!-- 有变更时：放弃 + 保存 -->
          <template v-if="isDirty">
            <Button type="text" size="small" @click="handleRevert">
              <template #icon><RollbackOutlined /></template>
              放弃更改
            </Button>
            <Button
              type="primary"
              size="small"
              :loading="saveStatus === 'saving'"
              @click="handleSave"
            >
              <SaveOutlined v-if="saveStatus !== 'saving'" />
              保存
            </Button>
          </template>
          <!-- 已保存/空闲：绿色对勾 -->
          <span v-else class="saved-indicator">
            <CheckCircleFilled style="font-size: 14px;" />
            <span>已保存</span>
          </span>
        </div>
      </div>

      <!-- 编辑器区域 -->
      <div class="editor-body">
        <SmartCodeEditor
          :model-value="editorContent"
          :show-toolbar="false"
          language="txt"
          theme="light"
          height="calc(100vh - 100px)"
          @update:model-value="handleContentChange"
          @save="handleEditorSave"
        />
      </div>
    </template>

    <!-- 加载中 -->
    <div v-else class="editor-loading">
      <LoadingOutlined spin style="font-size: 24px; color: #999" />
    </div>
  </div>
</template>

<style scoped>
.skill-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.editor-empty,
.editor-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  font-size: 14px;
  background: #fff;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;
}

.breadcrumb {
  font-size: 15px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.saved-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #52c41a;
  padding: 1.5px 0;
}

.saved-indicator .anticon {
  font-size: 13px;
}

.editor-body {
  flex: 1;
  overflow: hidden;
  background: #fff;
}
</style>
