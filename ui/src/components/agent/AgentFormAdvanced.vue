/**
 * 智能体高级设置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import StudioConfigSelect from '@/components/studio/StudioConfigSelect.vue'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    enablePlanning: boolean
    maxIterations: number
    maxSubtasks: number
    requirePlanConfirmation: boolean
    enableMemory: boolean
    showToolProcess: boolean
    enableMemoryCompression: boolean
    memoryCompressionConfig: Record<string, unknown> | null
    structuredOutputEnabled: boolean
    structuredOutputReminder: 'PROMPT' | 'TOOL_CHOICE'
    structuredOutputSchema: string
    studioConfigId: string | null
  }
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
}>()

const formRef = ref()

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 记忆压缩配置表单
 */
const memoryCompressionForm = computed({
  get: () => {
    if (!formData.value.memoryCompressionConfig) {
      return {
        maxToken: 131072,
        msgThreshold: 100,
        lastKeep: 50,
        tokenRatio: 0.75
      }
    }
    return formData.value.memoryCompressionConfig
  },
  set: (val) => {
    formData.value.memoryCompressionConfig = val
  }
})

/**
 * 表单验证规则
 */
const rules = computed(() => {
  const baseRules: Record<string, unknown[]> = {}

  if (formData.value.enablePlanning) {
    baseRules.maxIterations = [
      { required: true, message: '请输入最大迭代次数', trigger: 'blur' },
      { type: 'number', min: 1, max: 1000, message: '最大迭代次数范围: 1-1000', trigger: 'blur' }
    ]
    baseRules.maxSubtasks = [
      { required: true, message: '请输入最大子任务数', trigger: 'blur' },
      { type: 'number', min: 1, max: 100, message: '最大子任务数范围: 1-100', trigger: 'blur' }
    ]
  }

  return baseRules
})

/**
 * 处理显示工具调用过程开关
 */
function handleShowToolProcessToggle(checked: boolean) {
  formData.value.showToolProcess = checked
}

/**
 * 处理启用计划开关
 */
function handleEnablePlanningToggle(checked: boolean) {
  formData.value.enablePlanning = checked
  if (checked) {
    if (!formData.value.maxIterations) formData.value.maxIterations = 50
    if (!formData.value.maxSubtasks) formData.value.maxSubtasks = 10
  }
}

/**
 * 处理启用记忆压缩开关
 */
function handleEnableMemoryCompressionToggle(checked: boolean) {
  formData.value.enableMemoryCompression = checked
  if (checked && !formData.value.memoryCompressionConfig) {
    formData.value.memoryCompressionConfig = {
      maxToken: 131072,
      msgThreshold: 100,
      lastKeep: 50,
      tokenRatio: 0.75
    }
  } else if (!checked) {
    formData.value.memoryCompressionConfig = null
  }
}

/**
 * 处理结构化输出开关
 */
function handleStructuredOutputToggle(checked: boolean) {
  formData.value.structuredOutputEnabled = checked
  // if (!checked) {
  //   formData.value.structuredOutputSchema = ''
  // }
}

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

defineExpose({
  validate
})
</script>

<template>
  <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
    <AFormItem label="显示工具调用历史">
      <ASwitch
        v-model:checked="formData.showToolProcess"
        @change="handleShowToolProcessToggle"
      />
      <div class="text-placeholder text-xs mt-xs">
        开启后,在对话过程中将会看到工具的调用历史，包括输入和输出
      </div>
    </AFormItem>

    <AFormItem label="启用计划">
      <ASwitch
        v-model:checked="formData.enablePlanning"
        @change="handleEnablePlanningToggle"
      />
      <div class="text-placeholder text-xs mt-xs">
        开启后,智能体将具备任务规划能力,能够将复杂任务分解为多个子任务
      </div>
    </AFormItem>

    <div v-if="formData.enablePlanning" class="planning-config-section">
      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="最大迭代次数" name="maxIterations">
            <AInputNumber
              v-model:value="formData.maxIterations"
              :min="1"
              :max="1000"
              placeholder="默认50"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="最大子任务数" name="maxSubtasks">
            <AInputNumber
              v-model:value="formData.maxSubtasks"
              :min="1"
              :max="100"
              placeholder="默认10"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
      </ARow>

      <AFormItem label="需要计划确认">
        <ASwitch v-model:checked="formData.requirePlanConfirmation" />
        <div class="text-placeholder text-xs mt-xs">
          开启后,执行计划前需要用户确认
        </div>
      </AFormItem>
    </div>

    <AFormItem label="启用记忆">
      <ASwitch v-model:checked="formData.enableMemory" />
      <div class="text-placeholder text-xs mt-xs">
        开启后,智能体将能够记住对话历史
      </div>
    </AFormItem>

    <AFormItem v-if="formData.enableMemory" label="启用记忆压缩">
      <ASwitch
        v-model:checked="formData.enableMemoryCompression"
        @change="handleEnableMemoryCompressionToggle"
      />
      <div class="text-placeholder text-xs mt-xs">
        开启后,当记忆超过阈值时会自动压缩
      </div>
    </AFormItem>

    <div v-if="formData.enableMemory && formData.enableMemoryCompression" class="memory-config-section">
      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="最大Token数">
            <AInputNumber
              v-model:value="memoryCompressionForm.maxToken"
              :min="1024"
              :max="1000000"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="消息阈值">
            <AInputNumber
              v-model:value="memoryCompressionForm.msgThreshold"
              :min="10"
              :max="1000"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="保留最近消息数">
            <AInputNumber
              v-model:value="memoryCompressionForm.lastKeep"
              :min="1"
              :max="500"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="Token比率">
            <AInputNumber
              v-model:value="memoryCompressionForm.tokenRatio"
              :min="0.1"
              :max="1"
              :step="0.05"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
      </ARow>
    </div>

    <ARow :gutter="16">
      <ACol :span="12">
        <AFormItem label="启用结构化输出">
          <ASwitch
            v-model:checked="formData.structuredOutputEnabled"
            @change="handleStructuredOutputToggle"
          />
          <div class="text-placeholder text-xs mt-xs">
            开启后,智能体将按照指定的Schema输出结构化数据
          </div>
        </AFormItem>
      </ACol>
      <ACol :span="12" v-if="formData.structuredOutputEnabled">
        <AFormItem label="结构化输出模式">
          <ASelect
            ref="select"
            v-model:value="formData.structuredOutputReminder"
            :options="[
              {value: 'TOOL_CHOICE',label: 'TOOL_CHOICE（强制调用工具，一次 API 调用）'},
              {value: 'PROMPT',label: 'PROMPT（提示词引导，可能多次调用）'}
            ]"
          ></ASelect>
        </AFormItem>
      </ACol>
    </ARow>

    <AFormItem v-if="formData.structuredOutputEnabled" label="输出Schema">
      <SmartCodeEditor
        v-model="formData.structuredOutputSchema"
        language="json"
        height="250px"
      />
      <div class="text-placeholder text-xs mt-xs">
        定义输出的JSON Schema格式
      </div>
    </AFormItem>

    <AFormItem label="Studio 可视化调试">
      <StudioConfigSelect v-model="formData.studioConfigId" />
      <div class="text-placeholder text-xs mt-xs">
        配置Studio服务地址,用于智能体的可视化调试
      </div>
    </AFormItem>
  </AForm>
</template>

<style scoped lang="scss">
.planning-config-section,
.memory-config-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  margin-bottom: var(--spacing-md);
}
</style>
