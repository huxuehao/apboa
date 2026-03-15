/**
 * 钩子表单组件（自定义钩子）
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { LoginOutlined } from '@ant-design/icons-vue'
import type { HookConfigVO, HookConfig } from '@/types'
import { HookType } from '@/types'
import * as hookApi from '@/api/hook'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: HookConfigVO
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref()
const loading = ref<boolean>(false)

const formData = ref<{
  used?: string[]
  name: string
  description: string
  code: string
}>({
  name: '',
  description: '',
  code: ''
})

const codeTemplate = `import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CustomHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        return Mono.just(event);
    }
}`

const isEdit = ref<boolean>(false)

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.data) {
        isEdit.value = true
        formData.value = {
          used: props.data.used,
          name: props.data.name,
          description: props.data.description || '',
          code: props.data.code || codeTemplate
        }
      } else {
        resetForm()
      }
    }
  }
)

/**
 * 表单验证规则
 */
const rules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 100, message: '名称长度不能超过100个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' },
    { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入钩子代码', trigger: 'blur' }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  isEdit.value = false
  formData.value = {
    name: '',
    description: '',
    code: codeTemplate
  }
  formRef.value?.resetFields()
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true

    const entity: HookConfig = {
      name: formData.value.name,
      hookType: HookType.CUSTOM,
      description: formData.value.description,
      classPath: '',
      code: formData.value.code,
      priority: 0
    } as HookConfig

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      entity.enabled = props.data.enabled
      entity.classPath = props.data.classPath || ''
      entity.priority = props.data.priority ?? 0
      await hookApi.update(entity)
      message.success('更新成功')
    } else {
      await hookApi.save(entity)
      message.success('创建成功')
    }

    emit('success')
    handleCancel()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
  resetForm()
}
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="LoginOutlined"
    :title="isEdit ? '编辑钩子' : '新增钩子'"
    :confirm-loading="loading"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
      <AFormItem label="关联智能体" v-if="isEdit">
        <div class="code-wrapper">
          {{ formData?.used?.join('、') || '无' }}
        </div>
      </AFormItem>

      <AFormItem label="名称" name="name">
        <AInput v-model:value="formData.name" placeholder="请输入钩子名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入钩子描述"
          :rows="3"
        />
      </AFormItem>

      <AFormItem label="代码" name="code">
        <SmartCodeEditor
          v-if="visible"
          v-model="formData.code"
          language="java"
          height="350px"
        />
      </AFormItem>
    </AForm>
  </ApboaModal>
</template>
