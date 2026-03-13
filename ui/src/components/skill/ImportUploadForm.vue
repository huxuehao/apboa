/**
 * Zip 压缩包技能导入表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  InboxOutlined,
  FileZipOutlined,
  FolderOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import * as skillApi from '@/api/skill'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref()
const loading = ref(false)
const uploadFile = ref<File | null>(null)
const isDragging = ref(false)

const form = ref({
  category: 'Zip导入',
  cover: false
})

/**
 * 表单校验规则
 */
const rules = {
  category: [{ required: true, message: '请输入技能分类', trigger: 'blur' }]
}

/**
 * 重置表单
 */
function resetForm() {
  form.value = { category: 'Zip导入', cover: false }
  uploadFile.value = null
}

/**
 * 处理文件选择
 */
function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  validateAndSetFile(file)
  // 清空 input，使同一文件可重复选择
  input.value = ''
}

/**
 * 验证并设置文件
 */
function validateAndSetFile(file: File) {
  if (!file.name.endsWith('.zip')) {
    message.error('仅支持 .zip 格式的压缩包')
    return
  }
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    message.error('压缩包大小不能超过 100MB')
    return
  }
  uploadFile.value = file
}

/**
 * 处理拖拽放入
 */
function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    validateAndSetFile(file)
  }
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
  isDragging.value = true
}

function handleDragLeave() {
  isDragging.value = false
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * 清除已选文件
 */
function clearFile() {
  uploadFile.value = null
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!uploadFile.value) {
    message.warning('请先选择要上传的压缩包')
    return
  }
  try {
    await formRef.value?.validate()
    loading.value = true
    await skillApi.importFromUpload(uploadFile.value, form.value.category, form.value.cover)
    message.success('压缩包技能包导入成功')
    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 取消
 */
function handleCancel() {
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val) {
    resetForm()
  } else {
    formRef.value?.resetFields()
  }
})
</script>

<template>
  <AModal
    :open="visible"
    title="导入技能压缩包"
    width="580px"
    :confirm-loading="loading"
    ok-text="开始导入"
    cancel-text="取消"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <!-- 文件结构说明 -->
    <div class="struct-guide">
      <div class="struct-guide__header">
        <InfoCircleOutlined class="struct-guide__icon" />
        <span class="struct-guide__title">压缩包结构说明</span>
      </div>
      <div class="struct-guide__body">
        <p class="struct-guide__desc">请按以下结构组织压缩包内容，确保技能文件可以被正确识别和导入：</p>
        <div class="file-tree">
          <div class="file-tree__item file-tree__item--root">
            <FileZipOutlined class="file-tree__icon file-tree__icon--zip" />
            <span class="file-tree__name">your-skills.zip</span>
          </div>
          <div class="file-tree__item file-tree__item--l1">
            <FolderOutlined class="file-tree__icon file-tree__icon--folder" />
            <span class="file-tree__name">skills/</span>
            <span class="file-tree__tag">压缩包解压后必须存在此目录</span>
          </div>
          <div class="file-tree__item file-tree__item--l2">
            <FolderOutlined class="file-tree__icon file-tree__icon--file" />
            <span class="file-tree__name">技能一</span>
          </div>
          <div class="file-tree__item file-tree__item--l2">
            <FolderOutlined class="file-tree__icon file-tree__icon--file" />
            <span class="file-tree__name">技能一</span>
          </div>
          <div class="file-tree__item file-tree__item--l2">
            <span class="file-tree__name file-tree__name--more">... 更多技能文件夹</span>
          </div>
        </div>
      </div>
    </div>

    <AForm
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      class="import-form"
    >
      <!-- 文件上传区域 -->
      <AFormItem label="选择压缩包">
        <div
          class="upload-zone"
          :class="{ 'upload-zone--dragging': isDragging, 'upload-zone--filled': uploadFile }"
          @drop="handleDrop"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
        >
          <template v-if="!uploadFile">
            <input
              id="zip-upload-input"
              type="file"
              accept=".zip"
              class="upload-zone__input"
              @change="handleFileChange"
            />
            <label for="zip-upload-input" class="upload-zone__label">
              <InboxOutlined class="upload-zone__icon" />
              <p class="upload-zone__text">点击选择或拖拽压缩包到此处</p>
              <p class="upload-zone__hint">仅支持 .zip 格式，文件大小不超过 100MB</p>
            </label>
          </template>
          <template v-else>
            <div class="upload-file-info">
              <FileZipOutlined class="upload-file-info__icon" />
              <div class="upload-file-info__detail">
                <span class="upload-file-info__name">{{ uploadFile.name }}</span>
                <span class="upload-file-info__size">{{ formatFileSize(uploadFile.size) }}</span>
              </div>
              <AButton
                type="text"
                danger
                size="small"
                class="upload-file-info__remove"
                @click.stop="clearFile"
              >
                移除
              </AButton>
            </div>
          </template>
        </div>
      </AFormItem>

      <AFormItem label="技能分类" name="category">
        <AInput
          v-model:value="form.category"
          placeholder="请输入技能分类标签"
          allow-clear
        />
        <template #extra>导入的技能包将被归入此分类</template>
      </AFormItem>

      <AFormItem label="覆盖策略" name="cover">
        <div class="cover-option">
          <ASwitch v-model:checked="form.cover" />
          <span class="cover-option__label">{{ form.cover ? '覆盖已有同名技能' : '跳过已有同名技能' }}</span>
        </div>
        <template #extra>开启后，若存在同名技能包，将以新导入的内容覆盖原有数据</template>
      </AFormItem>
    </AForm>
  </AModal>
</template>

<style scoped lang="scss">
/* 结构说明区域 */
.struct-guide {
  margin-bottom: 20px;
  border: 1px solid var(--color-border-base, #e8e8e8);
  border-radius: 8px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: var(--color-bg-light, #f8f9fa);
    border-bottom: 1px solid var(--color-border-base, #e8e8e8);
  }

  &__icon {
    color: var(--color-primary);
    font-size: 15px;
  }

  &__title {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-base, #333);
  }

  &__body {
    padding: 12px 14px;
  }

  &__desc {
    margin: 0 0 10px;
    font-size: 12px;
    color: var(--color-text-secondary, #666);
  }

  //&__divider {
  //  margin-top: 8px;
  //  padding: 6px 10px;
  //  background: rgba(0, 0, 0, 0.03);
  //  border-radius: 4px;
  //  font-size: 12px;
  //  color: var(--color-text-secondary, #888);
  //  text-align: center;
  //}
}

/* 文件目录树 */
.file-tree {
  font-size: 12px;
  font-family: 'Consolas', 'Monaco', monospace;

  &__item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 3px 0;

    &--root { padding-left: 0; }
    &--l1 { padding-left: 20px; }
    &--l2 { padding-left: 40px; }
  }

  &__icon {
    font-size: 13px;
    flex-shrink: 0;

    &--zip { color: #e67e22; }
    &--folder { color: #f39c12; }
    &--file { color: #3498db; }
  }

  &__name {
    color: var(--color-text-base, #333);

    &--more {
      color: var(--color-text-secondary, #888);
      font-style: italic;
    }
  }

  &__tag {
    padding: 1px 6px;
    background: rgba(24, 144, 255, 0.1);
    color: var(--color-primary);
    border-radius: 4px;
    font-size: 11px;
  }
}

/* 上传区域 */
.upload-zone {
  position: relative;
  width: 100%;
  border: 2px dashed var(--color-border-base, #d9d9d9);
  border-radius: 8px;
  transition: all 0.2s;
  cursor: pointer;
  overflow: hidden;

  &--dragging {
    border-color: var(--color-primary);
    background: rgba(24, 144, 255, 0.04);
  }

  &--filled {
    border-style: solid;
    border-color: var(--color-primary);
    background: rgba(24, 144, 255, 0.03);
    cursor: default;
  }

  &:not(.upload-zone--filled):hover {
    border-color: var(--color-primary);
    background: rgba(24, 144, 255, 0.03);
  }

  &__input {
    position: absolute;
    inset: 0;
    opacity: 0;
    cursor: pointer;
    z-index: 1;
  }

  &__label {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px 16px;
    cursor: pointer;
  }

  &__icon {
    font-size: 36px;
    color: var(--color-primary);
    margin-bottom: 8px;
  }

  &__text {
    margin: 0 0 4px;
    font-size: 14px;
    color: var(--color-text-base, #333);
  }

  &__hint {
    margin: 0;
    font-size: 12px;
    color: var(--color-text-secondary, #999);
  }
}

/* 已选文件信息 */
.upload-file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;

  &__icon {
    font-size: 28px;
    color: #e67e22;
    flex-shrink: 0;
  }

  &__detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    overflow: hidden;
  }

  &__name {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-base, #333);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__size {
    font-size: 12px;
    color: var(--color-text-secondary, #999);
  }

  &__remove {
    flex-shrink: 0;
  }
}

/* 表单样式 */
.import-form {
  :deep(.ant-form-item-extra) {
    font-size: 12px;
    margin-top: 4px;
  }
}

.cover-option {
  display: flex;
  align-items: center;
  gap: 10px;

  &__label {
    font-size: 13px;
    color: var(--color-text-base, #333);
  }
}
</style>
