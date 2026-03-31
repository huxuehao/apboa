/**
 * 智能体表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import AgentFormBasic from './AgentFormBasic.vue'
import AgentFormModel from './AgentFormModel.vue'
import AgentFormTools from './AgentFormTools.vue'
import AgentFormKnowledge from './AgentFormKnowledge.vue'
import AgentFormAdvanced from './AgentFormAdvanced.vue'
import type { AgentDefinitionVO } from '@/types'
import { ToolChoiceStrategy } from '@/types'
import * as agentApi from '@/api/agent'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: AgentDefinitionVO
  tags: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const currentStep = ref(0)
const loading = ref(false)

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
    enableMemoryCompression: false,
    memoryCompressionConfig: null as Record<string, unknown> | null,
    structuredOutputEnabled: false,
    structuredOutputReminder: 'TOOL_CHOICE' as "TOOL_CHOICE" | "PROMPT",
    structuredOutputSchema: '',
    studioConfigId: null as string | null
  }
})

/**
 * 步骤配置
 */
const steps = [
  { title: '基本信息', description: '配置智能体的基本信息' },
  { title: '模型与提示词', description: '选择模型和提示词模板' },
  { title: '工具与能力', description: '配置钩子、工具、技能和敏感词' },
  { title: '知识库与MCP', description: '配置知识库、MCP服务器和子智能体' },
  { title: '高级设置', description: '配置计划、记忆、结构化输出和Studio' }
]

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

/**
 * 当前步骤引用
 */
const currentFormRef = computed(() => {
  switch (currentStep.value) {
    case 0:
      return basicFormRef.value
    case 1:
      return modelFormRef.value
    case 2:
      return toolsFormRef.value
    case 3:
      return knowledgeFormRef.value
    case 4:
      return advancedFormRef.value
    default:
      return null
  }
})

/**
 * 初始化表单数据
 */
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.data) {
        formData.value.basic = {
          name: props.data.name,
          agentCode: props.data.agentCode,
          description: props.data.description,
          tag: props.data.tag || ''
        }
        formData.value.model = {
          modelConfigId: props.data.modelConfigId,
          modelParamsOverride: props.data.modelParamsOverride || null,
          systemPromptTemplateId: props.data.systemPromptTemplateId,
          followTemplate: props.data.followTemplate,
          systemPrompt: props.data.systemPrompt
        }
        formData.value.tools = {
          hook: props.data.hook || [],
          toolChoiceStrategy: props.data.toolChoiceStrategy,
          tool: props.data.tool || [],
          specificToolName: props.data.specificToolName || '',
          skill: props.data.skill || [],
          sensitiveWordConfigId: props.data.sensitiveWordConfigId || '',
          codeExecutionConfigId: props.data.codeExecutionConfigId,
          sensitiveFilterEnabled: props.data.sensitiveFilterEnabled
        }
        formData.value.knowledge = {
          knowledgeBase: props.data.knowledgeBase || [],
          mcp: props.data.mcp || [],
          subAgent: props.data.subAgent || []
        }
        formData.value.advanced = {
          enablePlanning: props.data.enablePlanning,
          maxIterations: props.data.maxIterations || 50,
          maxSubtasks: props.data.maxSubtasks || 10,
          requirePlanConfirmation: props.data.requirePlanConfirmation,
          enableMemory: props.data.enableMemory,
          enableMemoryCompression: props.data.enableMemoryCompression,
          memoryCompressionConfig: props.data.memoryCompressionConfig || null,
          structuredOutputEnabled: props.data.structuredOutputEnabled,
          structuredOutputReminder: props.data.structuredOutputReminder || 'TOOL_CHOICE',
          structuredOutputSchema: props.data.structuredOutputSchema ? JSON.stringify(props.data.structuredOutputSchema, null, 2) : '',
          studioConfigId: props.data.studioConfigId || null
        }
      } else {
        resetForm()
      }
    }
  }
)

/**
 * 重置表单
 */
function resetForm() {
  currentStep.value = 0
  formData.value = {
    basic: {
      name: '',
      agentCode: '',
      description: '',
      tag: ''
    },
    model: {
      modelConfigId: '',
      modelParamsOverride: null,
      systemPromptTemplateId: '',
      followTemplate: true,
      systemPrompt: ''
    },
    tools: {
      hook: [],
      toolChoiceStrategy: ToolChoiceStrategy.AUTO,
      tool: [],
      specificToolName: '',
      skill: [],
      sensitiveWordConfigId: '',
      codeExecutionConfigId: '',
      sensitiveFilterEnabled: false
    },
    knowledge: {
      knowledgeBase: [],
      mcp: [],
      subAgent: []
    },
    advanced: {
      enablePlanning: false,
      maxIterations: 50,
      maxSubtasks: 10,
      requirePlanConfirmation: false,
      enableMemory: true,
      enableMemoryCompression: false,
      memoryCompressionConfig: null,
      structuredOutputEnabled: false,
      structuredOutputReminder: 'TOOL_CHOICE',
      structuredOutputSchema: '',
      studioConfigId: null
    }
  }
}

/**
 * 上一步
 */
function handlePrevious() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/**
 * 下一步
 */
async function handleNext() {
  const valid = await currentFormRef.value?.validate()
  if (valid && currentStep.value < steps.length - 1) {
    currentStep.value++
  }
}

/**
 * 改变步数
 */
async function handleStepChange(step: number) {
  const valid = await currentFormRef.value?.validate()
  if (valid) {
    currentStep.value = step
  }
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

    if (isEdit.value && props.data) {
      vo.id = props.data.id
      vo.enabled = props.data.enabled
      await agentApi.update(vo as AgentDefinitionVO)
      message.success('更新成功')
    } else {
      await agentApi.save(vo as AgentDefinitionVO)
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
 * 取消
 */
function handleCancel() {
  emit('update:visible', false)
  resetForm()
}
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="RobotOutlined"
    :title="isEdit ? '编辑智能体' : '新增智能体'"
    :footer="null"
    defaultWidth="100%"
    destroyOnClose
    @cancel="handleCancel"
  >
    <ASteps
      :current="currentStep"
      :items="steps"
      @change="handleStepChange"
      class="mb-lg"/>

    <div class="form-content" v-if="visible">
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
        :current-agent-id="data?.id"
      />

      <AgentFormAdvanced
        v-if="currentStep === 4"
        ref="advancedFormRef"
        style="width: calc(100% - 10px);"
        v-model="formData.advanced"
      />
    </div>

    <div class="form-actions flex justify-between mt-lg">
      <ASpace>
        <AButton :disabled="currentStep <= 0" @click="handlePrevious">
          上一步
        </AButton>
        <AButton :disabled="currentStep >= steps.length - 1" type="primary" @click="handleNext">
          下一步
        </AButton>
      </ASpace>

      <ASpace>
        <AButton @click="handleCancel">取消</AButton>
        <AButton :disabled="!isEdit && currentStep < steps.length - 1" type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '更新' : '创建' }}
        </AButton>
      </ASpace>
    </div>
  </ApboaModal>
</template>

<style scoped lang="scss">
.form-content {
  height: calc(100vh - 260px);
  overflow-y: auto;
  padding: var(--spacing-md) 0;
}

.form-actions {
  padding-top: var(--spacing-md);
}
</style>
