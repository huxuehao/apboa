/**
 * 智能体模型与提示词表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { RoutePaths } from '@/router/constants.ts'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import * as modelApi from '@/api/model'
import * as promptApi from '@/api/prompt'
import type { ModelProviderVO, ModelConfigVO, SystemPromptTemplateVO } from '@/types'
/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    modelConfigId: string
    modelParamsOverride: Record<string, unknown> | null
    systemPromptTemplateId: string
    followTemplate: boolean
    systemPrompt: string
  }
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
}>()

const formRef = ref()
const loading = ref(false)

const modelProviders = ref<ModelProviderVO[]>([])
const allModels = ref<ModelConfigVO[]>([])
const promptCategories = ref<string[]>([])
const allPrompts = ref<SystemPromptTemplateVO[]>([])

const selectedProviderId = ref<string>('')
const selectedPromptCategory = ref<string>('')
const showModelParamsOverride = ref(false)
const modelParamsForm = ref<Record<string, unknown>>({})

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 模型提供商选项
 */
const providerOptions = computed(() => {
  return modelProviders.value.map(p => ({
    label: p.name,
    value: p.id
  }))
})

/**
 * 当前提供商的模型列表
 */
const currentModels = computed(() => {
  if (!selectedProviderId.value) {
    const model = allModels.value.filter(p => p.id === formData.value.modelConfigId)[0] || null
    if (model) {
      selectedProviderId.value = model.providerId
    } else {
      selectedProviderId.value = allModels.value[0]?.providerId || ''
    }
    return []
  }
  return allModels.value.filter(m => m.providerId === selectedProviderId.value)
})

/**
 * 提示词分类选项
 */
const promptCategoryOptions = computed(() => {
  return promptCategories.value.map(c => ({
    label: c,
    value: c
  }))
})

/**
 * 当前分类的提示词列表
 */
const currentPrompts = computed(() => {
  if (!selectedPromptCategory.value) {
    const prompts = allPrompts.value.filter(p => p.id === formData.value.systemPromptTemplateId)[0] || null
    if (prompts) {
      selectedPromptCategory.value = prompts.category
    } else {
      selectedPromptCategory.value = allPrompts.value[0]?.category || ''
    }
    return []
  }

  return allPrompts.value.filter(p => p.category === selectedPromptCategory.value)
})

/**
 * 表单验证规则
 */
const rules = {
  modelConfigId: [
    { required: true, message: '请选择模型配置', trigger: 'blur' }
  ],
  systemPromptTemplateId: [
    { required: true, message: '请选择系统提示词模板', trigger: 'blur' }
  ]
}

/**
 * 加载模型提供商
 */
async function loadModelProviders() {
  try {
    loading.value = true
    const response = await modelApi.providerPage({ page: 1, size: 100, enabled: true })
    modelProviders.value = response.data.data.records || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载所有模型
 */
async function loadAllModels() {
  const response = await modelApi.configPage({ page: 1, size: 1000, enabled: true })
  allModels.value = response.data.data.records || []
}

/**
 * 加载提示词分类
 */
async function loadPromptCategories() {
  const response = await promptApi.listCategories()
  promptCategories.value = response.data.data || []
}

/**
 * 加载所有提示词
 */
async function loadAllPrompts() {
  const response = await promptApi.page({ page: 1, size: 1000, enabled: true })
  allPrompts.value = response.data.data.records || []
}

/**
 * 处理模型选择
 */
async function handleModelChange(modelId: string) {
  formData.value.modelConfigId = modelId

  if (showModelParamsOverride.value) {
    await loadModelParams(modelId)
  }
}

/**
 * 加载模型参数
 */
async function loadModelParams(modelId: string) {
  const response = await modelApi.configDetail(modelId)
  const model = response.data.data

  modelParamsForm.value = {
    streaming: model.streaming,
    temperature: model.temperature,
    topP: model.topP,
    topK: model.topK,
    maxTokens: model.maxTokens,
    repeatPenalty: model.repeatPenalty,
    seed: model.seed
  }

  formData.value.modelParamsOverride = { ...modelParamsForm.value }
}

/**
 * 处理覆盖模型参数开关
 */
function handleOverrideToggle(checked: boolean) {
  showModelParamsOverride.value = checked
  if (checked && formData.value.modelConfigId) {
    loadModelParams(formData.value.modelConfigId)
  } else {
    formData.value.modelParamsOverride = {}
    modelParamsForm.value = {}
  }
}

/**
 * 处理提示词模板选择
 */
async function handlePromptChange(promptId: string) {
  formData.value.systemPromptTemplateId = promptId

  if (!formData.value.followTemplate) {
    const response = await promptApi.detail(promptId)
    formData.value.systemPrompt = response.data.data.content
  }
}

/**
 * 处理随模板变化开关
 */
function handleFollowTemplateToggle(checked: boolean) {
  formData.value.followTemplate = checked
  if (!checked && formData.value.systemPromptTemplateId) {
    handlePromptChange(formData.value.systemPromptTemplateId)
  }
}

/**
 * 更新模型参数
 */
watch(modelParamsForm, (newVal) => {
  if (showModelParamsOverride.value) {
    formData.value.modelParamsOverride = { ...newVal }
  }
}, { deep: true })

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

onMounted(() => {
  loadModelProviders()
  loadAllModels()
  loadPromptCategories()
  loadAllPrompts()

  if (formData.value.modelConfigId) {
    const model = allModels.value.find(m => m.id === formData.value.modelConfigId)
    if (model) {
      selectedProviderId.value = model.providerId
    }
  }

  if (formData.value.systemPromptTemplateId) {
    const prompt = allPrompts.value.find(p => p.id === formData.value.systemPromptTemplateId)
    if (prompt) {
      selectedPromptCategory.value = prompt.category
    }
  }

  if (formData.value.modelParamsOverride) {
    showModelParamsOverride.value = true
    modelParamsForm.value = { ...formData.value.modelParamsOverride }
  }
})

defineExpose({
  validate
})
</script>

<template>
  <ASpin :spinning="loading">
    <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
      <AFormItem label="模型配置" name="modelConfigId" required>
        <template v-if="providerOptions?.length > 0">
          <div class="mb-md">
            <ASegmented
              v-model:value="selectedProviderId"
              :options="providerOptions"
              style="margin-bottom: 12px; background-color: var(--color-bg)"
            />
          </div>
          <ARadioGroup v-model:value="formData.modelConfigId" style="width: 100%">
            <div class="model-grid" v-if="currentModels?.length > 0">
              <ARadio
                v-for="model in currentModels"
                :key="model.id"
                :value="model.id"
                class="model-radio"
                @change="handleModelChange(model.id)"
              >
                <div class="model-info">
                  <div class="model-name">{{ model.name }}</div>
                  <div class="model-desc text-placeholder text-xs">{{ model.description }}</div>
                </div>
              </ARadio>
            </div>
            <div v-else class="text-placeholder mt-xs">
              <AButton type="text">未配置模型？</AButton>
              <AButton type="link" :href="`/#/${RoutePaths.MODEL}`" target="_blank">去配置</AButton>
              <AButton type="link" @click="loadModelProviders();loadAllModels()">刷新</AButton>
            </div>
          </ARadioGroup>
        </template>
        <template v-else>
          <div class="text-placeholder mt-xs">
            <AButton type="text">未添加模型提供商？</AButton>
            <AButton type="link" :href="`/#/${RoutePaths.MODEL}`" target="_blank">去添加</AButton>
            <AButton type="link" @click="loadModelProviders();loadAllModels()">刷新</AButton>
          </div>
        </template>
      </AFormItem>

      <AFormItem label="覆盖模型参数" v-if="formData.modelConfigId">
        <ASwitch
          :checked="showModelParamsOverride"
          @change="handleOverrideToggle"
        />
      </AFormItem>

      <div v-if="formData.modelConfigId && showModelParamsOverride" class="params-override-section">
        <ARow :gutter="16" :key="showModelParamsOverride">
          <ACol :span="12">
            <AFormItem label="Temperature">
              <AInputNumber
                v-model:value="modelParamsForm.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="Top P">
              <AInputNumber
                v-model:value="modelParamsForm.topP"
                :min="0"
                :max="1"
                :step="0.1"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="Top K">
              <AInputNumber
                v-model:value="modelParamsForm.topK"
                :min="0"
                :max="100"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="Max Tokens">
              <AInputNumber
                v-model:value="modelParamsForm.maxTokens"
                :min="1"
                :max="1000000"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="Repeat Penalty">
              <AInputNumber
                v-model:value="modelParamsForm.repeatPenalty"
                :min="0"
                :max="2"
                :step="0.1"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="随机种子 (Seed)" name="seed">
              <AInputNumber
                v-model:value="modelParamsForm.seed"
                style="width: 100%"
                placeholder="请输入随机种子，留空表示随机" />
            </AFormItem>
          </ACol>
          <ACol :span="12" />
          <ACol :span="12">
            <AFormItem label="Streaming">
              <ASwitch v-model:checked="modelParamsForm.streaming" />
            </AFormItem>
          </ACol>
        </ARow>
      </div>

      <AFormItem label="系统提示词模板" name="systemPromptTemplateId" required>
        <template v-if="promptCategoryOptions?.length > 0">
          <div class="mb-md">
            <ASegmented
              v-model:value="selectedPromptCategory"
              :options="promptCategoryOptions"
              style="margin-bottom: 12px; background-color: var(--color-bg)"
            />
          </div>
          <ARadioGroup v-model:value="formData.systemPromptTemplateId" style="width: 100%">
            <div class="prompt-grid" v-if="currentPrompts?.length > 0">
              <ARadio
                v-for="prompt in currentPrompts"
                :key="prompt.id"
                :value="prompt.id"
                class="prompt-radio"
                @change="handlePromptChange(prompt.id)"
              >
                <div class="prompt-info">
                  <div class="prompt-name">{{ prompt.name }}</div>
                  <div class="prompt-desc text-placeholder text-xs">{{ prompt.description }}</div>
                </div>
              </ARadio>
            </div>
            <div v-else class="text-placeholder mt-xs">
              <AButton type="text">没有有效的提示模板？</AButton>
              <AButton type="link" :href="`/#/${RoutePaths.PROMPT}`" target="_blank">去设置</AButton>
              <AButton type="link" @click="loadPromptCategories();loadAllPrompts()">刷新</AButton>
            </div>
          </ARadioGroup>
        </template>
        <template v-else>
          <div class="text-placeholder mt-xs">
            <AButton type="text">未添系统提示词模板？</AButton>
            <AButton type="link" :href="`/#/${RoutePaths.PROMPT}`" target="_blank">去添加</AButton>
            <AButton type="link" @click="loadPromptCategories();loadAllPrompts()">刷新</AButton>
          </div>
        </template>
      </AFormItem>

      <AFormItem label="随模板变化" v-if="formData.systemPromptTemplateId">
        <ASwitch
          v-model:checked="formData.followTemplate"
          @change="handleFollowTemplateToggle"
        />
        <div class="text-placeholder text-xs mt-xs">
          开启后,提示词内容将随模板更新而自动更新
        </div>
      </AFormItem>

      <AFormItem v-if="formData.systemPromptTemplateId && !formData.followTemplate" label="系统提示词">
        <SmartCodeEditor
          v-model="formData.systemPrompt"
          v-if="!formData.followTemplate"
          language="markdown"
          height="350px"
        />
      </AFormItem>
    </AForm>
  </ASpin>
</template>

<style scoped lang="scss">
.model-grid,
.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-sm);
}

.model-radio,
.prompt-radio {
  padding: var(--spacing-sm);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  margin: 0 !important;
  width: 100%;
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.model-info,
.prompt-info {
  .model-name,
  .prompt-name {
    font-weight: 500;
    margin-bottom: 4px;
  }

  .model-desc,
  .prompt-desc {
    line-height: 1.4;
  }
}

.params-override-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  margin-bottom: var(--spacing-md);
}
</style>
