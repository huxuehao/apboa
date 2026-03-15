/**
 * A2A 智能体表单组件（非分步、非全屏）
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { RobotOutlined } from '@ant-design/icons-vue'
import AgentFormBasic from './AgentFormBasic.vue'
import AgentA2aFormWellknown from './AgentA2aFormWellknown.vue'
import AgentA2aFormNacos from './AgentA2aFormNacos.vue'
import AgentA2aFormAdvanced from './AgentA2aFormAdvanced.vue'
import type { AgentDefinitionVO, WellKnownAgentConfig, NacosAgentConfig } from '@/types'
import { A2aType } from '@/types'
import * as agentApi from '@/api/agent'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: AgentDefinitionVO
  a2aType?: 'WELLKNOWN' | 'NACOS'
  tags: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const loading = ref(false)
const basicFormRef = ref()
const wellknownFormRef = ref()
const nacosFormRef = ref()

/**
 * 当前实际使用的 A2A 类型（编辑时从已加载配置中确定）
 */
const resolvedA2aType = ref<'WELLKNOWN' | 'NACOS'>('WELLKNOWN')

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

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
 * 增强配置
 */
const advancedData = ref({
  hook: [],
  enableMemory: false
})

/**
 * WellKnown 配置表单数据
 */
const wellknownData = ref<WellKnownAgentConfig>({
  agentName: '',
  baseUrl: '',
  relativeCardPath: '/.well-known/agent-card.json',
  authHeaders: []
})

/**
 * Nacos 配置表单数据
 */
const nacosData = ref<NacosAgentConfig>({
  agentName: '',
  nacosProperties: [
    { key: 'serverAddr', value: '', evn: false },
    { key: 'username', value: '', evn: false },
    { key: 'password', value: '', evn: false }
  ]
})

/**
 * 重置表单
 */
function resetForm() {
  basicData.value = { name: '', agentCode: '', description: '', tag: '' }
  wellknownData.value = {
    agentName: '',
    baseUrl: '',
    relativeCardPath: '/.well-known/agent-card.json',
    authHeaders: []
  }
  nacosData.value = {
    agentName: '',
    nacosProperties: [
      { key: 'serverAddr', value: '', evn: false },
      { key: 'username', value: '', evn: false },
      { key: 'password', value: '', evn: false }
    ]
  }
  advancedData.value = {
    hook: [],
    enableMemory: false
  }
  resolvedA2aType.value = props.a2aType || 'WELLKNOWN'
}

/**
 * 回填配置数据到表单
 *
 * @param config A2A 配置对象
 * @param type A2A 类型
 */
function fillA2aConfig(config: WellKnownAgentConfig | NacosAgentConfig, type: 'WELLKNOWN' | 'NACOS') {
  resolvedA2aType.value = type
  if (type === 'WELLKNOWN') {
    const c = config as WellKnownAgentConfig
    wellknownData.value = {
      agentName: c.agentName || '',
      baseUrl: c.baseUrl || '',
      relativeCardPath: c.relativeCardPath || '/.well-known/agent-card.json',
      authHeaders: c.authHeaders || []
    }
  } else {
    const c = config as NacosAgentConfig
    const props_list = c.nacosProperties || []
    // 确保固定行始终存在
    const fixedKeys = ['serverAddr']
    const merged = fixedKeys.map(k => {
      const found = props_list.find(p => p.key === k)
      return found || { key: k, value: '', evn: false }
    })
    const extras = props_list.filter(p => !fixedKeys.includes(p.key))
    nacosData.value = {
      agentName: c.agentName || '',
      nacosProperties: [...merged, ...extras]
    }
  }
}

/**
 * 监听弹框可见性变化，初始化或重置表单
 */
watch(
  () => props.visible,
  async (newVal) => {
    if (!newVal) return

    if (props.data?.id) {
      // 编辑模式：填充基本信息
      basicData.value = {
        name: props.data.name || '',
        agentCode: props.data.agentCode || '',
        description: props.data.description || '',
        tag: props.data.tag || ''
      }
      advancedData.value = {
        hook: props.data.hook || [],
        enableMemory: props.data.enableMemory || false
      }
      // 加载 A2A 配置
      try {
        const a2aRecord = props.data.agentA2A
        if (a2aRecord) {
          fillA2aConfig(
            a2aRecord.a2aConfig as WellKnownAgentConfig | NacosAgentConfig,
            a2aRecord.a2aType as 'WELLKNOWN' | 'NACOS'
          )
        }
      } catch (e) {
        console.error('加载 A2A 配置失败:', e)
      }
    } else {
      resetForm()
    }
  }
)

/**
 * 提交表单
 */
async function handleSubmit() {
  // 验证基本信息
  const basicValid = await basicFormRef.value?.validate()
  if (!basicValid) return

  // 验证 A2A 配置
  const a2aRef = resolvedA2aType.value === 'WELLKNOWN' ? wellknownFormRef.value : nacosFormRef.value
  const a2aValid = await a2aRef?.validate()
  if (!a2aValid) return

  loading.value = true
  try {
    // 获取 agentName 同步为 basicData.name
    const a2aConfigData = a2aRef.getFormData()
    a2aConfigData.agentName = basicData.value.agentCode

    // 构造基本信息 VO
    const vo: Partial<AgentDefinitionVO> = {
      name: basicData.value.name,
      agentCode: basicData.value.agentCode,
      description: basicData.value.description,
      tag: basicData.value.tag || '',
      agentType: 'A2A',
      hook: advancedData.value.hook ?? [],
      enableMemory: advancedData.value.enableMemory ?? false,
      agentA2A: {
        a2aType: resolvedA2aType.value === 'WELLKNOWN' ? A2aType.WELLKNOWN : A2aType.NACOS,
        a2aConfig: a2aConfigData
      }
    }

    if (isEdit.value && props.data) {
      vo.id = props.data.id
      vo.enabled = props.data.enabled
      await agentApi.update(vo as AgentDefinitionVO)
    } else {
      await agentApi.save(vo as AgentDefinitionVO)
    }
    message.success(isEdit.value ? '更新成功' : '创建成功')
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
    :title="isEdit ? '编辑 A2A 智能体' : `新增 A2A 智能体 · ${resolvedA2aType === 'WELLKNOWN' ? 'WellKnown' : 'Nacos'}`"
    destroyOnClose
    :confirm-loading="loading"
    :okText="isEdit ? '更新' : '创建'"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AAlert
      message="说明：因 AgentScope 未适配 AgentCard 的 securitySchemes，目前只能调用非鉴权 A2A-Server"
      closable
      style="margin-bottom: 20px"
    />
    <div class="a2a-form-body" v-if="visible">
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
          <ATag :color="resolvedA2aType === 'WELLKNOWN' ? 'blue' : 'red'" class="ml-sm" :bordered="false">
            {{ resolvedA2aType === 'WELLKNOWN' ? 'WellKnown' : 'Nacos' }}
          </ATag>
        </div>

        <AgentA2aFormWellknown
          v-if="resolvedA2aType === 'WELLKNOWN'"
          ref="wellknownFormRef"
          v-model="wellknownData"
        />

        <AgentA2aFormNacos
          v-else
          ref="nacosFormRef"
          v-model="nacosData"
        />
      </div>

      <!-- 基本信息 -->
      <div class="form-section">
        <div class="section-title">增强配置</div>
        <AgentA2aFormAdvanced
          v-model="advancedData"
        />
      </div>
    </div>
  </ApboaModal>
</template>

<style scoped lang="scss">
.a2a-form-body {
  .form-section {
    .section-title {
      font-size: var(--font-size-base);
      font-weight: 600;
      color: var(--color-text-primary);
      margin-bottom: var(--spacing-md);
      display: flex;
      align-items: center;
    }
  }
}

.form-actions {
  padding-top: var(--spacing-sm);
}
</style>
