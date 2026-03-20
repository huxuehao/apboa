/**
 * Studio配置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { StudioConfig, StudioConfigVO } from '@/types'
import * as studioApi from '@/api/studio'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: StudioConfigVO
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

/**
 * 表单引用
 */
const formRef = ref()

/**
 * 表单数据
 */
const formData = ref({
  url: '',
  project: ''
})

/**
 * 提交中状态
 */
const submitting = ref(false)

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

/**
 * 表单验证规则
 */
const rules = {
  url: [
    { required: true, message: '请输入URL', trigger: 'blur' }
  ],
  project: [
    { required: true, message: '请输入项目名称', trigger: 'blur' }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    url: '',
    project: ''
  }
  formRef.value?.resetFields()
}

/**
 * 监听弹窗显示状态
 */
watch(() => props.visible, (val) => {
  if (val) {
    if (props.data) {
      formData.value = {
        url: props.data.url,
        project: props.data.project
      }
    } else {
      resetForm()
    }
  }
})

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const entity: StudioConfig = {
      url: formData.value.url,
      project: formData.value.project
    }

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      await studioApi.update(entity)
      message.success('更新成功')
    } else {
      await studioApi.save(entity)
      message.success('创建成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
}
</script>

<template>
  <AModal
    :open="visible"
    :title="isEdit ? '编辑Studio配置' : '新增Studio配置'"
    :confirm-loading="submitting"
    :destroy-on-close="true"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AForm
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
    >
      <AFormItem label="URL" name="url">
        <AInput v-model:value="formData.url" placeholder="请输入Studio服务URL" />
      </AFormItem>

      <AFormItem label="项目名称" name="project">
        <AInput v-model:value="formData.project" placeholder="请输入项目名称" />
      </AFormItem>
    </AForm>
  </AModal>
</template>
