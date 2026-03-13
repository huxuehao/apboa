/**
 * 本地技能包导入表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FolderOpenOutlined, SyncOutlined } from '@ant-design/icons-vue'
import * as skillApi from '@/api/skill'
import type { LocalImportConfig } from '@/types'

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

const form = ref<LocalImportConfig>({
  category: '本地导入',
  path: '.apboa/skills',
  cover: false
})

/**
 * 表单校验规则
 */
const rules = {
  category: [{ required: true, message: '请输入技能分类', trigger: 'blur' }],
  path: [{ required: true, message: '请输入本地路径', trigger: 'blur' }]
}

/**
 * 重置表单
 */
function resetForm() {
  form.value = {
    category: '本地导入',
    path: '.apboa/skills',
    cover: false
  }
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await skillApi.importFromLocal(form.value)
    message.success('本地技能包导入成功')
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
    title="装载本地技能包"
    width="520px"
    :confirm-loading="loading"
    ok-text="开始导入"
    cancel-text="取消"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <div class="import-desc">
      <FolderOpenOutlined class="import-desc__icon" />
      <div class="import-desc__text">
        <p>从服务器本地文件系统路径中扫描并导入技能包文件。路径需为服务端可访问的目录，默认指向 <code>.apboa/skills</code> 目录。</p>
      </div>
    </div>

    <AForm
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      class="import-form"
    >
      <AFormItem label="技能分类" name="category">
        <AInput
          v-model:value="form.category"
          placeholder="请输入技能分类标签"
          allow-clear
        />
        <template #extra>导入的技能包将被归入此分类</template>
      </AFormItem>

      <AFormItem label="本地路径" name="path">
        <AInput
          v-model:value="form.path"
          placeholder="请输入服务端本地目录路径"
          allow-clear
        >
          <template #prefix>
            <FolderOpenOutlined class="text-secondary" />
          </template>
        </AInput>
        <template #extra>请填写服务端可访问的目录绝对路径或相对路径</template>
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
.import-desc {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 20px;
  background: var(--color-bg-light, #f8f9fa);
  border: 1px solid var(--color-border-base, #e8e8e8);
  border-radius: 8px;

  &__icon {
    font-size: 18px;
    color: var(--color-primary);
    margin-top: 2px;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;

    p {
      margin: 0;
      font-size: 13px;
      color: var(--color-text-secondary, #666);
      line-height: 1.6;

      code {
        padding: 1px 6px;
        background: rgba(0, 0, 0, 0.06);
        border-radius: 4px;
        font-size: 12px;
        font-family: monospace;
      }
    }
  }
}

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
