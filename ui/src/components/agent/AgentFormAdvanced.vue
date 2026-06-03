/**
 * 智能体高级设置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
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
    enableLongTermMemory: boolean
    longTermMemoryConfig: Record<string, unknown> | null
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
 * 长期记忆类型选项
 */
const memoryTypeOptions = [
  { value: 'MEM0', label: 'Mem0 长期记忆' },
  { value: 'REME', label: 'ReMe 长期记忆（阿里通义千问）' },
  { value: 'BAILIAN', label: '百炼记忆库（阿里云百炼）' }
]

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
 * 获取指定记忆类型的默认配置
 */
function getDefaultMemoryConfig(type: string): Record<string, unknown> {
  switch (type) {
    case 'MEM0':
      return {
        memoryType: 'MEM0',
        apiBaseUrl: 'https://api.mem0.ai',
        apiKey: '',
        apiType: 'platform',
        memoryMode: 'BOTH'
      }
    case 'REME':
      return {
        memoryType: 'REME',
        apiBaseUrl: 'https://api.reme.ai',
        memoryMode: 'BOTH',
        timeout: 30
      }
    case 'BAILIAN':
      return {
        memoryType: 'BAILIAN',
        apiKey: '',
        memoryLibraryId: '',
        projectId: '',
        memoryMode: 'BOTH',
        topK: 5,
        minScore: 0.5,
        enableRerank: false,
        enableJudge: false,
        enableRewrite: false
      }
    default:
      return {
        memoryType: 'MEM0',
        apiBaseUrl: 'https://api.mem0.ai',
        apiKey: '',
        apiType: 'platform',
        memoryMode: 'BOTH'
      }
  }
}

/**
 * 处理启用长期记忆开关
 */
function handleEnableLongTermMemoryToggle(checked: boolean) {
  formData.value.enableLongTermMemory = checked
  if (checked && !formData.value.longTermMemoryConfig) {
    // 首次开启，初始化为按类型存储的配置结构
    formData.value.longTermMemoryConfig = {
      MEM0: getDefaultMemoryConfig('MEM0'),
      REME: getDefaultMemoryConfig('REME'),
      BAILIAN: getDefaultMemoryConfig('BAILIAN'),
      memoryType: 'MEM0'
    }
  } else if (!checked) {
    formData.value.longTermMemoryConfig = null
  }
}

/**
 * 当前记忆类型
 */
const currentMemoryType = computed({
  get: () => {
    return (formData.value.longTermMemoryConfig as Record<string, unknown>)?.memoryType as string || 'MEM0'
  },
  set: (val: string) => {
    if (!formData.value.longTermMemoryConfig) {
      formData.value.longTermMemoryConfig = {}
    }
    (formData.value.longTermMemoryConfig as Record<string, unknown>).memoryType = val
  }
})

/**
 * 获取当前类型对应的记忆配置
 * 使用 ref 确保深层属性变更的响应式
 */
const currentMemoryConfig = ref<Record<string, unknown> | null>(null)

/**
 * 非空记忆配置（模板中使用），当 currentMemoryConfig 为 null 时返回空对象
 */
const memoryConfig = computed(() => currentMemoryConfig.value ?? {} as Record<string, unknown>)

/**
 * 从嵌套结构中同步当前类型的子配置到 currentMemoryConfig
 */
function syncCurrentMemoryConfig() {
  const config = formData.value.longTermMemoryConfig as Record<string, unknown> | null
  if (!config) {
    currentMemoryConfig.value = null
    return
  }
  const type = config.memoryType as string || 'MEM0'
  const subConfig = config[type] as Record<string, unknown> | undefined
  if (subConfig) {
    currentMemoryConfig.value = subConfig
  } else {
    const defaultConfig = getDefaultMemoryConfig(type)
    config[type] = defaultConfig
    currentMemoryConfig.value = defaultConfig
  }
}

// 监听 longTermMemoryConfig 变化，同步 currentMemoryConfig
watch(
  () => formData.value.longTermMemoryConfig,
  () => syncCurrentMemoryConfig(),
  { deep: true, immediate: true }
)

// 监听 memoryType 变化，同步 currentMemoryConfig
watch(
  () => (formData.value.longTermMemoryConfig as Record<string, unknown> | null)?.memoryType,
  () => syncCurrentMemoryConfig()
)

/**
 * 处理记忆类型切换
 */
function handleMemoryTypeChange(type: string) {
  const config = formData.value.longTermMemoryConfig as Record<string, unknown> | null
  if (!config) return

  // 如果该类型还没有配置，初始化默认值
  if (!config[type]) {
    config[type] = getDefaultMemoryConfig(type)
  }
  // 切换当前类型
  config.memoryType = type
}


/**
 * 根据当前记忆类型返回对应的服务地址提示
 */
const apiBaseUrlPlaceholder = computed(() => {
  const type = currentMemoryType.value
  switch (type) {
    case 'MEM0': return 'https://api.mem0.ai'
    case 'REME': return 'https://api.reme.ai'
    case 'BAILIAN': return 'https://bailian.aliyuncs.com'
    default: return 'https://api.mem0.ai'
  }
})

const apiBaseUrlHint = computed(() => {
  const type = currentMemoryType.value
  switch (type) {
    case 'MEM0': return 'Platform Mem0: https://api.mem0.ai，自建 Mem0: http://localhost:8000'
    case 'REME': return '阿里通义千问 ReMe 记忆服务地址'
    case 'BAILIAN': return '阿里云百炼平台服务地址'
    default: return ''
  }
})

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
            :max="5000"
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

    <!-- 长期记忆 -->
    <ADivider />
    <AFormItem label="启用长期记忆">
      <ASwitch
        v-model:checked="formData.enableLongTermMemory"
        @change="handleEnableLongTermMemoryToggle"
      />
      <div class="text-placeholder text-xs mt-xs">
        跨会话长期记忆，智能体可记住用户偏好、重要事实等，实现个性化交互
      </div>
    </AFormItem>

    <!-- 长期记忆子配置 -->
    <div v-if="formData.enableLongTermMemory" class="config-section">
      <!-- 记忆类型选择 -->
      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="记忆类型">
            <ASelect
              :value="currentMemoryType"
              @change="handleMemoryTypeChange"
              style="width: 100%"
            >
              <ASelectOption
                v-for="opt in memoryTypeOptions"
                :key="opt.value"
                :value="opt.value"
              >{{ opt.label }}</ASelectOption>
            </ASelect>
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="记忆控制模式">
            <ASelect
              v-model:value="memoryConfig.memoryMode"
              :default-value="'BOTH'"
              style="width: 100%"
            >
              <ASelectOption value="AGENT_CONTROL">Agent 自主控制</ASelectOption>
              <ASelectOption value="STATIC_CONTROL">系统自动管理</ASelectOption>
              <ASelectOption value="BOTH">两者结合</ASelectOption>
            </ASelect>
          </AFormItem>
        </ACol>
      </ARow>

      <!-- 通用配置：服务地址 -->
      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="服务地址" name="longTermMemoryConfig.apiBaseUrl">
            <AInput
              v-model:value="memoryConfig.apiBaseUrl"
              :placeholder="apiBaseUrlPlaceholder"
            />
            <div class="text-placeholder text-xs mt-xs">{{ apiBaseUrlHint }}</div>
          </AFormItem>
        </ACol>
      </ARow>

      <!-- ===== Mem0 专属配置 ===== -->
      <template v-if="currentMemoryType === 'MEM0'">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="API 密钥" name="longTermMemoryConfig.apiKey">
              <AInputPassword
                v-model:value="memoryConfig.apiKey"
                placeholder="输入 Mem0 API Key"
                autocomplete="new-password"
              />
              <div class="text-placeholder text-xs mt-xs">Platform Mem0 必需，自建 Mem0 可选</div>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="部署类型">
              <ASelect
                v-model:value="memoryConfig.apiType"
                :default-value="'platform'"
                style="width: 100%"
              >
                <ASelectOption value="platform">Platform Mem0</ASelectOption>
                <ASelectOption value="self-hosted">自建 Mem0</ASelectOption>
              </ASelect>
            </AFormItem>
          </ACol>
        </ARow>
      </template>

      <!-- ===== ReMe 专属配置 ===== -->
      <template v-if="currentMemoryType === 'REME'">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="请求超时（秒）">
              <AInputNumber
                v-model:value="memoryConfig.timeout"
                :min="5"
                :max="300"
                :default-value="30"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
        </ARow>
      </template>

      <!-- ===== 百炼记忆库专属配置 ===== -->
      <template v-if="currentMemoryType === 'BAILIAN'">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="API 密钥" name="longTermMemoryConfig.apiKey">
              <AInputPassword
                v-model:value="memoryConfig.apiKey"
                placeholder="输入阿里云 API Key"
                autocomplete="new-password"
              />
              <div class="text-placeholder text-xs mt-xs">阿里云百炼平台 API Key</div>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="记忆库 ID" name="longTermMemoryConfig.memoryLibraryId">
              <AInput
                v-model:value="memoryConfig.memoryLibraryId"
                placeholder="输入记忆库 ID"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="项目 ID" name="longTermMemoryConfig.projectId">
              <AInput
                v-model:value="memoryConfig.projectId"
                placeholder="输入项目 ID"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="用户画像 Schema">
              <AInput
                v-model:value="memoryConfig.profileSchema"
                placeholder="用户画像 Schema（可选）"
              />
            </AFormItem>
          </ACol>
          <ACol :span="8">
            <AFormItem label="TopK">
              <AInputNumber
                v-model:value="memoryConfig.topK"
                :min="1"
                :max="100"
                :default-value="5"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="8">
            <AFormItem label="最低匹配分数">
              <AInputNumber
                v-model:value="memoryConfig.minScore"
                :min="0"
                :max="1"
                :step="0.1"
                :default-value="0.5"
                style="width: 100%"
              />
            </AFormItem>
          </ACol>
          <ACol :span="8">
            <AFormItem label="启用重排序">
              <ASwitch v-model:checked="memoryConfig.enableRerank" />
            </AFormItem>
          </ACol>
          <ACol :span="8">
            <AFormItem label="启用判断">
              <ASwitch v-model:checked="memoryConfig.enableJudge" />
            </AFormItem>
          </ACol>
          <ACol :span="8">
            <AFormItem label="启用改写">
              <ASwitch v-model:checked="memoryConfig.enableRewrite" />
            </AFormItem>
          </ACol>
        </ARow>
      </template>
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
