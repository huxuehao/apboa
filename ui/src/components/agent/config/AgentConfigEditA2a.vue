/**
 * A2A智能体配置-编辑子组件
 * 支持WellKnown和Nacos两种A2A协议类型
 *
 * @component
 */
<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import AgentFormBasic from '@/components/agent/AgentFormBasic.vue'
import AgentA2aFormWellknown from '@/components/agent/AgentA2aFormWellknown.vue'
import AgentA2aFormNacos from '@/components/agent/AgentA2aFormNacos.vue'
import AgentA2aFormAdvanced from '@/components/agent/AgentA2aFormAdvanced.vue'
import type { AgentDefinitionVO, WellKnownAgentConfig, NacosAgentConfig } from '@/types'
import { A2aType } from '@/types'
import * as agentApi from '@/api/agent'

const props = defineProps<{
  agentData: AgentDefinitionVO
  tags: string[]
}>()

const emit = defineEmits<{
  success: []
}>()

const loading = ref(false)
const isDirty = ref(false)

const basicFormRef = ref()
const wellknownFormRef = ref()
const nacosFormRef = ref()
const advancedFormRef = ref()

/**
 * 当前A2A类型
 */
const a2aType = ref<A2aType>(A2aType.WELLKNOWN)

/**
 * 基本信息表单数据
 */
const basicData = ref({
  name: '',
  agentCode: '',
  description: '',
  tag: ''
})

/**
 * 增强配置数据
 */
const advancedData = ref({
  hook: [] as string[],
  enableMemory: false
})

/**
 * WellKnown配置数据
 */
const wellknownData = ref<WellKnownAgentConfig>({
  agentName: '',
  baseUrl: '',
  relativeCardPath: '/.well-known/agent-card.json',
  authHeaders: []
})

/**
 * Nacos配置数据
 */
const nacosData = ref<NacosAgentConfig>({
  agentName: '',
  nacosProperties: [
    { key: 'serverAddr', value: '', evn: false }
  ]
})

/**
 * 初始化表单数据
 */
function initFormData() {
  const data = props.agentData
  if (!data) return

  basicData.value = {
    name: data.name || '',
    agentCode: data.agentCode || '',
    description: data.description || '',
    tag: data.tag || ''
  }

  advancedData.value = {
    hook: data.hook || [],
    enableMemory: data.enableMemory || false
  }

  // 初始化 A2A 配置
  const a2aRecord = data.agentA2A
  if (a2aRecord) {
    a2aType.value = a2aRecord.a2aType
    if (a2aRecord.a2aType === A2aType.WELLKNOWN) {
      const config = a2aRecord.a2aConfig as WellKnownAgentConfig
      wellknownData.value = {
        agentName: config.agentName || '',
        baseUrl: config.baseUrl || '',
        relativeCardPath: config.relativeCardPath || '/.well-known/agent-card.json',
        authHeaders: config.authHeaders || []
      }
    } else {
      const config = a2aRecord.a2aConfig as NacosAgentConfig
      const propsList = config.nacosProperties || []
      const fixedKeys = ['serverAddr']
      const merged = fixedKeys.map(k => {
        const found = propsList.find(p => p.key === k)
        return found || { key: k, value: '', evn: false }
      })
      const extras = propsList.filter(p => !fixedKeys.includes(p.key))
      nacosData.value = {
        agentName: config.agentName || '',
        nacosProperties: [...merged, ...extras]
      }
    }
  }

  nextTick(() => {
    isDirty.value = false
  })
}

watch(() => props.agentData, () => {
  initFormData()
}, { immediate: true })

watch([basicData, advancedData, wellknownData, nacosData], () => {
  isDirty.value = true
}, { deep: true })

/**
 * 当前A2A表单ref
 */
const currentA2aFormRef = computed(() => {
  return a2aType.value === A2aType.WELLKNOWN ? wellknownFormRef.value : nacosFormRef.value
})

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    const basicValid = await basicFormRef.value?.validate()
    if (!basicValid) return

    const a2aValid = await currentA2aFormRef.value?.validate()
    if (!a2aValid) return

    loading.value = true

    const a2aConfigData = currentA2aFormRef.value.getFormData()
    a2aConfigData.agentName = basicData.value.agentCode

    const vo: Partial<AgentDefinitionVO> = {
      id: props.agentData.id,
      enabled: props.agentData.enabled,
      name: basicData.value.name,
      agentCode: basicData.value.agentCode,
      description: basicData.value.description,
      tag: basicData.value.tag || '',
      agentType: 'A2A',
      hook: advancedData.value.hook ?? [],
      enableMemory: advancedData.value.enableMemory ?? false,
      agentA2A: {
        a2aType: a2aType.value === A2aType.WELLKNOWN ? A2aType.WELLKNOWN : A2aType.NACOS,
        a2aConfig: a2aConfigData
      }
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
  <div class="config-edit-a2a">
    <div class="config-edit-a2a-form">
      <!-- 基本信息 -->
      <div class="form-section">
        <div class="section-title">基本信息</div>
        <AgentFormBasic
          ref="basicFormRef"
          v-model="basicData"
          :tags="tags"
        />
      </div>

      <!-- A2A 配置 -->
      <div class="form-section">
        <div class="section-title">
          A2A 配置
          <ATag :color="a2aType === A2aType.WELLKNOWN ? 'blue' : 'red'" class="ml-sm" :bordered="false">
            {{ a2aType === A2aType.WELLKNOWN ? 'WellKnown' : 'Nacos' }}
          </ATag>
        </div>

        <AgentA2aFormWellknown
          v-if="a2aType === A2aType.WELLKNOWN"
          ref="wellknownFormRef"
          v-model="wellknownData"
        />

        <AgentA2aFormNacos
          v-else
          ref="nacosFormRef"
          v-model="nacosData"
        />
      </div>

      <!-- 增强配置 -->
      <div class="form-section">
        <div class="section-title">增强配置</div>
        <AgentA2aFormAdvanced
          ref="advancedFormRef"
          v-model="advancedData"
        />
      </div>
    </div>

    <div class="config-edit-a2a-actions flex justify-center mt-lg">
      <AButton type="primary" :loading="loading" @click="handleSubmit">保存</AButton>
    </div>
  </div>
</template>

<style scoped lang="scss">
.config-edit-a2a {
  height: 100%;
  background-color: #FFFFFF;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.config-edit-a2a-form {
  flex: 1;
  overflow-y: auto;
}

.form-section {
  margin-bottom: var(--spacing-lg);

  .section-title {
    font-size: var(--font-size-base);
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: var(--spacing-md);
    display: flex;
    align-items: center;
  }
}

.config-edit-a2a-actions {
  padding-top: var(--spacing-md);
}
</style>
