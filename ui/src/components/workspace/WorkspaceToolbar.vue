<script setup lang="ts">
import { ref, h } from 'vue'
import {
  UploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  ClearOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'

/**
 * 工作空间工具栏组件
 */
defineProps<{
  /** 是否处于多选模式 */
  multiSelect: boolean
  /** 已选中的文件数量 */
  selectedCount: number
  /** 工作空间是否有文件 */
  hasFiles: boolean
  /** 是否正在上传 */
  uploading: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-multi-select'): void
  (e: 'upload', files: File[]): void
  (e: 'delete'): void
  (e: 'download'): void
  (e: 'clear'): void
  (e: 'refresh'): void
}>()

/** 隐藏的文件输入元素 */
const fileInputRef = ref<HTMLInputElement | null>(null)

/** 刷新按钮旋转状态 */
const refreshSpinning = ref(false)

/**
 * 触发刷新，添加旋转动画
 */
const handleRefresh = () => {
  refreshSpinning.value = true
  emit('refresh')
  setTimeout(() => { refreshSpinning.value = false }, 600)
}

/**
 * 触发文件选择对话框
 */
const triggerUpload = () => {
  fileInputRef.value?.click()
}

/**
 * 处理文件选择
 */
const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (files.length) {
    emit('upload', files)
  }
  // 重置 input，允许重复选择同一文件
  input.value = ''
}

/**
 * 确认清空工作空间
 */
const handleClear = () => {
  Modal.confirm({
    title: '确认清空',
    content: '将删除工作空间内所有文件，此操作不可恢复，是否继续？',
    okText: '确认清空',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: () => emit('clear'),
  })
}
</script>

<template>
  <div class="workspace-toolbar">
    <!-- 隐藏的文件选择 input -->
    <input
      ref="fileInputRef"
      type="file"
      multiple
      style="display: none"
      @change="handleFileChange"
    />

    <!-- 左侧：多选 -->
    <div class="workspace-toolbar-left">
      {{ selectedCount ? '已选择（' + selectedCount + '）' : '未选择' }}
    </div>

    <!-- 右侧：刷新、上传、下载、删除、清空 -->
    <div class="workspace-toolbar-right">
      <!-- 刷新 -->
      <ATooltip placement="bottom" title="刷新">
        <AButton
          type="text"
          :class="{ 'is-spinning': refreshSpinning }"
          :icon="h(ReloadOutlined)"
          @click="handleRefresh"
        ></AButton>
      </ATooltip>
      <!-- 上传 -->
      <ATooltip placement="bottom" title="上传（支持压缩包）">
        <AButton
          type="text"
          :disabled="uploading"
          :icon="h(UploadOutlined)"
          @click="triggerUpload"
        ></AButton>
      </ATooltip>
      <!-- 下载 -->
      <ATooltip placement="bottom" title="下载">
        <AButton
          type="text"
          :disabled="selectedCount === 0"
          :icon="h(DownloadOutlined)"
          @click="$emit('download')"
        ></AButton>
      </ATooltip>
      <!-- 删除 -->
      <ATooltip placement="bottom" title="删除">
        <AButton
          type="text"
          danger
          :disabled="selectedCount === 0"
          :icon="h(DeleteOutlined)"
          @click="$emit('delete')"
        ></AButton>
      </ATooltip>
      <!-- 清空 -->
      <ATooltip placement="bottom" title="清空">
        <AButton
          type="text"
          :disabled="!hasFiles"
          :icon="h(ClearOutlined)"
          @click="handleClear"
        ></AButton>
      </ATooltip>

    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/workspace.scss' as *;
</style>
