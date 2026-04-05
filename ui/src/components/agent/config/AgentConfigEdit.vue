/**
 * 智能体配置-编辑子组件
 * 复用 AgentForm 的多步表单逻辑
 *
 * @component
 */
<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import AgentFormBasic from '@/components/agent/AgentFormBasic.vue'
import AgentFormModel from '@/components/agent/AgentFormModel.vue'
import AgentFormTools from '@/components/agent/AgentFormTools.vue'
import AgentFormKnowledge from '@/components/agent/AgentFormKnowledge.vue'
import AgentFormAdvanced from '@/components/agent/AgentFormAdvanced.vue'
import type { AgentDefinitionVO } from '@/types'
import { ToolChoiceStrategy } from '@/types'
import * as agentApi from '@/api/agent'

const props = defineProps<{
  agentData: AgentDefinitionVO
  tags: string[]
}>()

const emit = defineEmits<{
  success: []
}>()

const currentStep = ref(0)
const loading = ref(false)
const isDirty = ref(false)

const basicFormRef = ref()
const modelFormRef = ref()
const toolsFormRef = ref()
const knowledgeFormRef = ref()
const advancedFormRef = ref()

/**
 * 表单数据
 */
const formData = ref({
  basic: {
    name: '',
    agentCode: '',
    description: '',
    tag: ''
  },
  model: {
    modelConfigId: '',
    modelParamsOverride: null as Record<string, unknown> | null,
    systemPromptTemplateId: '',
    followTemplate: true,
    systemPrompt: ''
  },
  tools: {
    hook: [] as string[],
    toolChoiceStrategy: ToolChoiceStrategy.AUTO,
    tool: [] as string[],
    specificToolName: '',
    skill: [] as string[],
    codeExecutionConfigId: null as string | null,
    sensitiveWordConfigId: '',
    sensitiveFilterEnabled: false
  },
  knowledge: {
    knowledgeBase: [] as string[],
    mcp: [] as string[],
    subAgent: [] as string[]
  },
  advanced: {
    enablePlanning: false,
    maxIterations: 50,
    maxSubtasks: 10,
    requirePlanConfirmation: false,
    enableMemory: true,
    showToolProcess: true,
    enableMemoryCompression: false,
    memoryCompressionConfig: null as Record<string, unknown> | null,
    structuredOutputEnabled: false,
    structuredOutputReminder: 'TOOL_CHOICE' as "TOOL_CHOICE" | "PROMPT",
    structuredOutputSchema: '',
    studioConfigId: null as string | null
  }
})

const steps = [
  { title: '基本信息', description: '配置智能体的基本信息' },
  { title: '模型与提示词', description: '选择模型和提示词模板' },
  { title: '工具与能力', description: '配置钩子、工具、技能和敏感词' },
  { title: '知识库与MCP', description: '配置知识库、MCP服务器和子智能体' },
  { title: '高级设置', description: '配置计划、记忆、结构化输出和Studio' }
]

const currentFormRef = computed(() => {
  switch (currentStep.value) {
    case 0: return basicFormRef.value
    case 1: return modelFormRef.value
    case 2: return toolsFormRef.value
    case 3: return knowledgeFormRef.value
    case 4: return advancedFormRef.value
    default: return null
  }
})

/**
 * 初始化表单数据
 */
function initFormData() {
  const data = props.agentData
  if (!data) return

  formData.value.basic = {
    name: data.name,
    agentCode: data.agentCode,
    description: data.description,
    tag: data.tag || ''
  }
  formData.value.model = {
    modelConfigId: data.modelConfigId,
    modelParamsOverride: data.modelParamsOverride || null,
    systemPromptTemplateId: data.systemPromptTemplateId,
    followTemplate: data.followTemplate,
    systemPrompt: data.systemPrompt
  }
  formData.value.tools = {
    hook: data.hook || [],
    toolChoiceStrategy: data.toolChoiceStrategy,
    tool: data.tool || [],
    specificToolName: data.specificToolName || '',
    skill: data.skill || [],
    sensitiveWordConfigId: data.sensitiveWordConfigId || '',
    codeExecutionConfigId: data.codeExecutionConfigId,
    sensitiveFilterEnabled: data.sensitiveFilterEnabled
  }
  formData.value.knowledge = {
    knowledgeBase: data.knowledgeBase || [],
    mcp: data.mcp || [],
    subAgent: data.subAgent || []
  }
  formData.value.advanced = {
    enablePlanning: data.enablePlanning,
    maxIterations: data.maxIterations || 50,
    maxSubtasks: data.maxSubtasks || 10,
    requirePlanConfirmation: data.requirePlanConfirmation,
    enableMemory: data.enableMemory,
    showToolProcess: data.showToolProcess,
    enableMemoryCompression: data.enableMemoryCompression,
    memoryCompressionConfig: data.memoryCompressionConfig || null,
    structuredOutputEnabled: data.structuredOutputEnabled,
    structuredOutputReminder: data.structuredOutputReminder || 'TOOL_CHOICE',
    structuredOutputSchema: data.structuredOutputSchema ? JSON.stringify(data.structuredOutputSchema, null, 2) : '',
    studioConfigId: data.studioConfigId || null
  }

  nextTick(() => {
    isDirty.value = false
  })
}

watch(() => props.agentData, () => {
  initFormData()
}, { immediate: true })

watch(formData, () => {
  isDirty.value = true
}, { deep: true })

async function handlePrevious() {
  if (currentStep.value > 0) currentStep.value--
}

async function handleNext() {
  const valid = await currentFormRef.value?.validate()
  if (valid && currentStep.value < steps.length - 1) currentStep.value++
}

async function handleStepChange(step: number) {
  const valid = await currentFormRef.value?.validate()
  if (valid) currentStep.value = step
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    const valid = await currentFormRef.value?.validate()
    if (!valid) return

    loading.value = true

    const vo: Partial<AgentDefinitionVO> = {
      id: props.agentData.id,
      enabled: props.agentData.enabled,
      name: formData.value.basic.name,
      agentType: 'CUSTOM',
      agentCode: formData.value.basic.agentCode,
      description: formData.value.basic.description,
      tag: formData.value.basic.tag || '',
      modelConfigId: formData.value.model.modelConfigId,
      modelParamsOverride: formData.value.model.modelParamsOverride,
      systemPromptTemplateId: formData.value.model.systemPromptTemplateId,
      followTemplate: formData.value.model.followTemplate,
      systemPrompt: formData.value.model.systemPrompt,
      toolChoiceStrategy: formData.value.tools.toolChoiceStrategy,
      tool: formData.value.tools.tool,
      specificToolName: formData.value.tools.specificToolName,
      skill: formData.value.tools.skill,
      sensitiveWordConfigId: formData.value.tools.sensitiveWordConfigId,
      sensitiveFilterEnabled: formData.value.tools.sensitiveFilterEnabled,
      knowledgeBase: formData.value.knowledge.knowledgeBase,
      mcp: formData.value.knowledge.mcp,
      subAgent: formData.value.knowledge.subAgent,
      hook: formData.value.tools.hook,
      enablePlanning: formData.value.advanced.enablePlanning,
      maxIterations: formData.value.advanced.maxIterations,
      maxSubtasks: formData.value.advanced.maxSubtasks,
      requirePlanConfirmation: formData.value.advanced.requirePlanConfirmation,
      showToolProcess: formData.value.advanced.showToolProcess,
      enableMemory: formData.value.advanced.enableMemory,
      enableMemoryCompression: formData.value.advanced.enableMemoryCompression,
      memoryCompressionConfig: formData.value.advanced.memoryCompressionConfig,
      structuredOutputEnabled: formData.value.advanced.structuredOutputEnabled,
      structuredOutputReminder: formData.value.advanced.structuredOutputReminder,
      structuredOutputSchema: formData.value.advanced.structuredOutputEnabled && formData.value.advanced.structuredOutputSchema
        ? JSON.parse(formData.value.advanced.structuredOutputSchema)
        : null,
      studioConfigId: formData.value.advanced.studioConfigId,
      codeExecutionConfigId: formData.value.tools.codeExecutionConfigId,
      version: '1.0.0'
    }

    await agentApi.update(vo as AgentDefinitionVO)
    message.success('更新成功')
    isDirty.value = false
    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

defineExpose({ isDirty })
</script>

<template>
  <div class="config-edit">
    <ASteps
      :current="currentStep"
      :items="steps"
      @change="handleStepChange"
      class="mb-lg"
    />

    <div class="config-edit-form">
      <AgentFormBasic
        v-if="currentStep === 0"
        ref="basicFormRef"
        v-model="formData.basic"
        :tags="tags"
      />
      <AgentFormModel
        v-if="currentStep === 1"
        ref="modelFormRef"
        v-model="formData.model"
      />
      <AgentFormTools
        v-if="currentStep === 2"
        ref="toolsFormRef"
        v-model="formData.tools"
      />
      <AgentFormKnowledge
        v-if="currentStep === 3"
        ref="knowledgeFormRef"
        v-model="formData.knowledge"
        :current-agent-id="agentData?.id"
      />
      <AgentFormAdvanced
        v-if="currentStep === 4"
        ref="advancedFormRef"
        style="width: calc(100% - 10px);"
        v-model="formData.advanced"
      />
    </div>

    <div class="config-edit-actions flex justify-between mt-lg">
      <ASpace>
        <AButton :disabled="currentStep <= 0" @click="handlePrevious">上一步</AButton>
        <AButton :disabled="currentStep >= steps.length - 1" type="primary" @click="handleNext">下一步</AButton>
      </ASpace>
      <AButton type="primary" :loading="loading" @click="handleSubmit">保存</AButton>
    </div>
  </div>
</template>

<style scoped lang="scss">
.config-edit {
  height: 100%;
  background-color: #FFFFFF;
  padding: 12px 24px;
}
.config-edit-form {
  height: calc(100vh - 280px);
  overflow-y: auto;
  padding: var(--spacing-md) 0;
}

.config-edit-actions {
  padding-top: var(--spacing-md);
}
</style>
