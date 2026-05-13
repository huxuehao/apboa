/**
 * 智能体高级设置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import StudioConfigSelect from '@/components/studio/StudioConfigSelect.vue'
import CodeExecutionConfigSelect from "@/components/codeExecution/CodeExecutionConfigSelect.vue";
import {InfoCircleOutlined} from "@ant-design/icons-vue";

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
    codeExecutionConfigId: string | null
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
        tokenRatio: 0.75,
        minCompressionTokenThreshold: 5000,
        currentRoundCompressionRatio: 0.3,
        minConsecutiveToolMessages: 6,
        offloadSinglePreview: 200,
        largePayloadThreshold: 5120,
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
  baseRules.maxIterations = [
    { required: true, message: '请输入最大迭代次数', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '最大迭代次数范围: 1-1000', trigger: 'blur' }
  ]
  if (formData.value.enablePlanning) {
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
    <!-- Row 1: 迭代次数 & 执行环境 -->
    <ARow :gutter="16">
      <ACol :span="12">
        <AFormItem name="maxIterations">
          <template #label>
            <ATooltip title="智能体遵循「思考 - 执行」循环机制，单次完整循环即为一次迭代，用于限制最大循环次数，防止无限执行">
              <span>最大迭代次数</span><InfoCircleOutlined class="text-secondary cursor-pointer" />
            </ATooltip>
          </template>
          <AInputNumber
            v-model:value="formData.maxIterations"
            :min="1"
            :max="1000"
            style="width: 50%"
            placeholder="默认50"
          />
          <div class="text-placeholder text-xs mt-xs">
            设置「思考 - 执行」循环的最大次数，如果该智能体需要执行复杂任务，可能需要增加该值
          </div>
        </AFormItem>
      </ACol>
      <ACol :span="12">
        <AFormItem>
          <template #label>
            <ATooltip title="配置后可开启工作空间，赋予智能体执行 Shell 脚本、读取&写入文件的能力">
              <span>执行环境配置</span><InfoCircleOutlined class="text-secondary cursor-pointer" />
            </ATooltip>
          </template>
          <CodeExecutionConfigSelect v-model="formData.codeExecutionConfigId"/>
          <div class="text-placeholder text-xs mt-xs">
            如果您希望 Skill 中的脚本可以被正常执行，那么请确保已正确配置此项
          </div>
        </AFormItem>
      </ACol>
    </ARow>

    <!-- Row 2: Studio & 计划能力 -->
    <ARow :gutter="16">
      <ACol :span="12">
        <AFormItem label="Studio 可视化调试">
          <StudioConfigSelect v-model="formData.studioConfigId" />
          <div class="text-placeholder text-xs mt-xs">配置Studio服务地址，用于智能体可视化调试</div>
        </AFormItem>
      </ACol>
      <ACol :span="12">
        <AFormItem label="启用计划能力">
          <ASwitch
            v-model:checked="formData.enablePlanning"
            @change="handleEnablePlanningToggle"
          />
          <div class="text-placeholder text-xs mt-xs">开启后智能体可将复杂任务分解为多个子任务</div>
        </AFormItem>
      </ACol>
    </ARow>

    <!-- 计划能力子配置 -->
    <div v-if="formData.enablePlanning" class="config-section">
      <ARow :gutter="16">
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
        <ACol :span="12">
          <AFormItem label="需要确认计划">
            <ASwitch v-model:checked="formData.requirePlanConfirmation" :disabled="!formData.enableMemory" />
            <div class="text-placeholder text-xs mt-xs">需先开启记忆方可生效</div>
          </AFormItem>
        </ACol>
      </ARow>
    </div>

    <!-- Row 3: 记忆 & 工具调用历史 -->
    <ARow :gutter="16">
      <ACol :span="12">
        <AFormItem label="开启记忆">
          <ASwitch v-model:checked="formData.enableMemory" />
          <div class="text-placeholder text-xs mt-xs">记住对话历史并持久化到数据库</div>
        </AFormItem>
      </ACol>
      <ACol :span="12">
        <AFormItem label="显示工具调用历史">
          <ASwitch
            v-model:checked="formData.showToolProcess"
            @change="handleShowToolProcessToggle"
          />
          <div class="text-placeholder text-xs mt-xs">对话中显示工具调用的输入与输出</div>
        </AFormItem>
      </ACol>
    </ARow>

    <!-- 记忆压缩开关 -->
    <AFormItem v-if="formData.enableMemory" label="开启记忆压缩">
      <ASwitch
        v-model:checked="formData.enableMemoryCompression"
        @change="handleEnableMemoryCompressionToggle"
      />
      <div class="text-placeholder text-xs mt-xs">记忆超过阈值时自动压缩</div>
    </AFormItem>

    <!-- 记忆压缩子配置 -->
    <div v-if="formData.enableMemory && formData.enableMemoryCompression" class="config-section">
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
        <ACol :span="12">
          <AFormItem label="大负载阈值">
            <AInputNumber
              v-model:value="memoryCompressionForm.largePayloadThreshold"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="单预览卸载阈值">
            <AInputNumber
              v-model:value="memoryCompressionForm.offloadSinglePreview"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="最小连续工具消息数">
            <AInputNumber
              v-model:value="memoryCompressionForm.minConsecutiveToolMessages"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="当前轮压缩比">
            <AInputNumber
              v-model:value="memoryCompressionForm.currentRoundCompressionRatio"
              :min="0.1"
              :max="1"
              :step="0.1"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="最小压缩令牌阈值">
            <AInputNumber
              v-model:value="memoryCompressionForm.minCompressionTokenThreshold"
              style="width: 100%"
            />
          </AFormItem>
        </ACol>
      </ARow>
    </div>
  </AForm>
</template>

<style scoped lang="scss">
.config-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  margin-bottom: var(--spacing-md);
}
</style>
