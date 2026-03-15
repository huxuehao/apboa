/**
 * 模型供应商表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ApiOutlined } from '@ant-design/icons-vue'
import type { ModelProviderVO, ModelProvider } from '@/types'
import { ModelProviderType, AuthType } from '@/types'
import * as modelApi from '@/api/model'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  modelProviderType?: ModelProviderType
  data?: ModelProviderVO
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
  type: ModelProviderType
  name: string
  description: string
  baseUrl: string
  authType: AuthType
  apiKey: string
  envVarName: string
}>({
  type: ModelProviderType.OPEN_AI,
  name: '',
  description: '',
  baseUrl: '',
  authType: AuthType.CONFIG,
  apiKey: '',
  envVarName: ''
})

const isEdit = computed(() => !!props.data?.id)

/**
 * 供应商类型选项
 */
const providerTypeOptions = [
  { label: 'DashScope', value: ModelProviderType.DASH_SCOPE },
  { label: 'OpenAI', value: ModelProviderType.OPEN_AI },
  { label: 'Anthropic', value: ModelProviderType.ANTHROPIC },
//   { label: 'Gemini', value: ModelProviderType.GEMINI },
  { label: 'Ollama', value: ModelProviderType.OLLAMA }
]

/**
 * 认证类型选项
 */
const authTypeOptions = [
  { label: '直接配置', value: AuthType.CONFIG },
  { label: '环境变量', value: AuthType.ENV }
]

/**
 * 获取供应商类型对应的默认 Base URL
 */
function getDefaultBaseUrl(type: ModelProviderType): string {
  const baseUrlMap: Record<ModelProviderType, string> = {
    [ModelProviderType.DASH_SCOPE]: 'https://dashscope.aliyuncs.com',
    [ModelProviderType.OPEN_AI]: 'https://api.openai.com',
    [ModelProviderType.ANTHROPIC]: 'https://api.anthropic.com',
    [ModelProviderType.OLLAMA]: 'http://localhost:11434',
    [ModelProviderType.GEMINI]: 'http://localhost:8080'
  }
  return baseUrlMap[type] || ''
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.data) {
        formData.value = {
          type: props.data.type as ModelProviderType,
          name: props.data.name,
          description: props.data.description,
          baseUrl: props.data.baseUrl,
          authType: props.data.authType,
          apiKey: props.data.apiKey || '',
          envVarName: props.data.envVarName || ''
        }
      } else {
        resetForm()
      }
    }
  }
)

/**
 * 监听供应商类型变化，自动设置默认 baseUrl
 */
watch(
  () => formData.value.type,
  (newType) => {
    if (!isEdit.value) {
      formData.value.baseUrl = getDefaultBaseUrl(newType)
    }
  }
)

/**
 * 表单验证规则
 */
const rules = computed(() => {
  const baseRules: Record<string, Array<{ required?: boolean; message?: string; trigger?: string; max?: number }>> = {
    type: [
      { required: true, message: '请选择供应商类型', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入名称', trigger: 'blur' },
      { max: 100, message: '名称长度不能超过100个字符', trigger: 'blur' }
    ],
    description: [
      { required: true, message: '请输入描述', trigger: 'blur' },
      { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }
    ],
    baseUrl: [
      { required: true, message: '请输入Base URL', trigger: 'blur' },
      { max: 200, message: 'Base URL长度不能超过200个字符', trigger: 'blur' }
    ],
    authType: [
      { required: true, message: '请选择认证类型', trigger: 'blur' }
    ]
  }

  if (formData.value.authType === AuthType.CONFIG) {
    baseRules.apiKey = [
      { required: true, message: '请输入API密钥', trigger: 'blur' },
      { max: 500, message: 'API密钥长度不能超过500个字符', trigger: 'blur' }
    ]
  }

  if (formData.value.authType === AuthType.ENV) {
    baseRules.envVarName = [
      { required: true, message: '请输入环境变量名', trigger: 'blur' },
      { max: 100, message: '环境变量名长度不能超过100个字符', trigger: 'blur' }
    ]
  }

  return baseRules
})

/**
 * 重置表单
 */
function resetForm() {
  const providerType = props.modelProviderType || ModelProviderType.OPEN_AI
  formData.value = {
    type: providerType,
    name: '',
    description: '',
    baseUrl: getDefaultBaseUrl(providerType),
    authType: AuthType.CONFIG,
    apiKey: '',
    envVarName: ''
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

    const entity: ModelProvider = {
      type: formData.value.type,
      name: formData.value.name,
      description: formData.value.description,
      baseUrl: formData.value.baseUrl,
      authType: formData.value.authType,
      apiKey: formData.value.authType === AuthType.CONFIG ? formData.value.apiKey : '',
      envVarName: formData.value.authType === AuthType.ENV ? formData.value.envVarName : '',
      configMeta: null
    } as ModelProvider

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      await modelApi.providerUpdate(entity)
      message.success('更新成功')
    } else {
      await modelApi.providerSave(entity)
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
    :title-icon="ApiOutlined"
    :title="isEdit ? '编辑供应商' : '新增供应商'"
    :confirm-loading="loading"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
      <AFormItem label="供应商类型" name="type">
        <ASelect v-model:value="formData.type" placeholder="请选择供应商类型">
          <ASelectOption v-for="opt in providerTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </ASelectOption>
        </ASelect>
      </AFormItem>

      <AFormItem label="名称" name="name">
        <AInput v-model:value="formData.name" placeholder="请输入供应商名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入供应商描述"
          :rows="3"
        />
      </AFormItem>

      <AFormItem label="Base URL" name="baseUrl">
        <AInput v-model:value="formData.baseUrl" placeholder="请输入API基础地址" />
      </AFormItem>

      <AFormItem label="认证类型" name="authType">
        <ARadioGroup v-model:value="formData.authType">
          <ARadio v-for="opt in authTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </ARadio>
        </ARadioGroup>
        <div class="auth-type-tip text-placeholder text-xs mt-xs">
          <div v-if="formData.authType === 'CONFIG'">
            直接在配置中输入API密钥,密钥将加密存储在数据库中
          </div>
          <div v-else>
            使用环境变量存储API密钥,更安全。需要在系统环境变量中配置对应的变量名
          </div>
        </div>
      </AFormItem>

      <AFormItem v-if="formData.authType === 'CONFIG'" label="API密钥" name="apiKey">
        <AInputPassword v-model:value="formData.apiKey" placeholder="请输入API密钥" />
      </AFormItem>

      <AFormItem v-if="formData.authType === 'ENV'" label="环境变量名" name="envVarName">
        <AInput v-model:value="formData.envVarName" placeholder="请输入环境变量名，如: OPENAI_API_KEY" />
      </AFormItem>
    </AForm>
  </ApboaModal>
</template>

<style scoped lang="scss">
.auth-type-tip {
  line-height: 1.5;
}
</style>
