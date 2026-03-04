/**
 * 扩展配置编辑器
 * 用于编辑 headers、queryParams、bodyParams
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'

export interface ExtendConfigData {
  headers: Record<string, string>
  queryParams: Record<string, string>
  bodyParams: Record<string, unknown>
  /** 固定系统消息 */
  fixedSystemMessage?: boolean
}

interface KeyValueItem {
  key: string
  value: string
}

interface BodyParamItem {
  key: string
  value: string
  valueType: 'string' | 'number' | 'boolean'
}

const props = withDefaults(
  defineProps<{
    modelValue: ExtendConfigData | null
    /** 是否紧凑模式（用于 AgentFormModel 等嵌套场景） */
    compact?: boolean
    /** 是否显示固定系统消息配置项（仅 OpenAI 供应商显示） */
    showFixedSystemMessage?: boolean
  }>(),
  { compact: false, showFixedSystemMessage: false }
)

const emit = defineEmits<{
  'update:modelValue': [value: ExtendConfigData | null]
}>()

/** 内部编辑结构 - 使用 ref 以便可编辑 */
const headersList = ref<KeyValueItem[]>([{ key: '', value: '' }])
const queryParamsList = ref<KeyValueItem[]>([{ key: '', value: '' }])
const bodyParamsList = ref<BodyParamItem[]>([{ key: '', value: '', valueType: 'string' }])
const fixedSystemMessage = ref(false)

watch(
  () => props.modelValue,
  (val) => {
    headersList.value = recordToKeyValueList(val?.headers)
    queryParamsList.value = recordToKeyValueList(val?.queryParams)
    bodyParamsList.value = recordToBodyParamList(val?.bodyParams)
    fixedSystemMessage.value = val?.fixedSystemMessage ?? false
  },
  { immediate: true }
)

function syncHeaders() {
  emitUpdate('headers', keyValueListToRecord(headersList.value))
}

function syncQueryParams() {
  emitUpdate('queryParams', keyValueListToRecord(queryParamsList.value))
}

function syncBodyParams() {
  emitUpdate('bodyParams', bodyParamListToRecord(bodyParamsList.value))
}

function recordToKeyValueList(record: Record<string, string> | undefined): KeyValueItem[] {
  if (!record || Object.keys(record).length === 0) return [{ key: '', value: '' }]
  return Object.entries(record).map(([key, value]) => ({ key, value }))
}

function keyValueListToRecord(list: KeyValueItem[]): Record<string, string> {
  const record: Record<string, string> = {}
  list.forEach(({ key, value }) => {
    if (key?.trim()) record[key.trim()] = value ?? ''
  })
  return Object.keys(record).length > 0 ? record : {}
}

function recordToBodyParamList(record: Record<string, unknown> | undefined): BodyParamItem[] {
  if (!record || Object.keys(record).length === 0) return [{ key: '', value: '', valueType: 'string' }]
  return Object.entries(record).map(([key, value]) => ({
    key,
    value: String(value),
    valueType: (typeof value === 'number' ? 'number' : typeof value === 'boolean' ? 'boolean' : 'string') as BodyParamItem['valueType']
  }))
}

function bodyParamListToRecord(list: BodyParamItem[]): Record<string, unknown> {
  const record: Record<string, unknown> = {}
  list.forEach(({ key, value, valueType }) => {
    if (!key?.trim()) return
    const v = valueType === 'number' ? Number(value) : valueType === 'boolean' ? value === 'true' : value
    record[key.trim()] = v
  })
  return Object.keys(record).length > 0 ? record : {}
}

function emitUpdate(field: keyof ExtendConfigData, value: Record<string, unknown> | Record<string, string>) {
  const current = props.modelValue || { headers: {}, queryParams: {}, bodyParams: {}, fixedSystemMessage: false }
  const next = { ...current, [field]: value, fixedSystemMessage: fixedSystemMessage.value }
  const hasData = Object.keys(next.headers).length > 0 || Object.keys(next.queryParams).length > 0 || Object.keys(next.bodyParams).length > 0 || next.fixedSystemMessage === true
  emit('update:modelValue', hasData ? next : null)
}

function syncFixedSystemMessage() {
  const current = props.modelValue || { headers: {}, queryParams: {}, bodyParams: {} }
  const next = { ...current, fixedSystemMessage: fixedSystemMessage.value }
  const hasData = Object.keys(next.headers).length > 0 || Object.keys(next.queryParams).length > 0 || Object.keys(next.bodyParams).length > 0 || next.fixedSystemMessage === true
  emit('update:modelValue', hasData ? next : null)
}

function addRow(field: 'headers' | 'queryParams' | 'bodyParams') {
  if (field === 'bodyParams') {
    bodyParamsList.value.push({ key: '', value: '', valueType: 'string' })
    syncBodyParams()
  } else if (field === 'headers') {
    headersList.value.push({ key: '', value: '' })
    syncHeaders()
  } else {
    queryParamsList.value.push({ key: '', value: '' })
    syncQueryParams()
  }
}

function removeRow(field: 'headers' | 'queryParams' | 'bodyParams', index: number) {
  if (field === 'bodyParams') {
    bodyParamsList.value.splice(index, 1)
    if (bodyParamsList.value.length === 0) bodyParamsList.value.push({ key: '', value: '', valueType: 'string' })
    syncBodyParams()
  } else if (field === 'headers') {
    headersList.value.splice(index, 1)
    if (headersList.value.length === 0) headersList.value.push({ key: '', value: '' })
    syncHeaders()
  } else {
    queryParamsList.value.splice(index, 1)
    if (queryParamsList.value.length === 0) queryParamsList.value.push({ key: '', value: '' })
    syncQueryParams()
  }
}


const bodyValueTypeOptions = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' }
]
</script>

<template>
  <div class="extend-config-editor" :class="{ compact }">
    <div v-if="showFixedSystemMessage" class="fixed-system-message-row">
      <ASwitch v-model:checked="fixedSystemMessage" @change="syncFixedSystemMessage" />
      <div class="label-wrap">
        <span class="label">固定系统消息</span>
        <span class="text-placeholder text-xs">确保 system 消息始终在消息列表的最前面，以兼容 SGLang 等严格部署环境</span>
      </div>
    </div>
    <ACollapse :bordered="false" :default-active-key="['headers', 'queryParams', 'bodyParams']">
      <ACollapsePanel key="headers" header="请求头 (Headers)">
        <div class="param-list">
          <div
            v-for="(item, index) in headersList"
            :key="`h-${index}`"
            class="param-row"
          >
            <AInput v-model:value="item.key" placeholder="Header 名称" class="param-key" @blur="syncHeaders" />
            <AInput v-model:value="item.value" placeholder="Header 值" class="param-value" @blur="syncHeaders" />
            <AButton type="text" danger size="small" class="param-remove" @click="removeRow('headers', index)">
              <MinusCircleOutlined />
            </AButton>
          </div>
          <AButton type="dashed" block size="small" @click="addRow('headers')">
            <PlusOutlined /> 添加请求头
          </AButton>
        </div>
      </ACollapsePanel>

      <ACollapsePanel key="queryParams" header="查询参数 (Query Params)">
        <div class="param-list">
          <div
            v-for="(item, index) in queryParamsList"
            :key="`q-${index}`"
            class="param-row"
          >
            <AInput v-model:value="item.key" placeholder="参数名" class="param-key" @blur="syncQueryParams" />
            <AInput v-model:value="item.value" placeholder="参数值" class="param-value" @blur="syncQueryParams" />
            <AButton type="text" danger size="small" class="param-remove" @click="removeRow('queryParams', index)">
              <MinusCircleOutlined />
            </AButton>
          </div>
          <AButton type="dashed" block size="small" @click="addRow('queryParams')">
            <PlusOutlined /> 添加查询参数
          </AButton>
        </div>
      </ACollapsePanel>

      <ACollapsePanel key="bodyParams" header="请求体参数 (Body Params)">
        <div class="param-list">
          <div
            v-for="(item, index) in bodyParamsList"
            :key="`b-${index}`"
            class="param-row param-row-body"
          >
            <AInput v-model:value="item.key" placeholder="参数名" class="param-key" @blur="syncBodyParams" />
            <ASelect v-model:value="item.valueType" :options="bodyValueTypeOptions" class="param-type" @change="syncBodyParams" />
            <AInput
              v-model:value="item.value"
              :placeholder="item.valueType === 'boolean' ? 'true / false' : item.valueType === 'number' ? '数字' : '字符串值'"
              class="param-value"
              @blur="syncBodyParams"
            />
            <AButton type="text" danger size="small" class="param-remove" @click="removeRow('bodyParams', index)">
              <MinusCircleOutlined />
            </AButton>
          </div>
          <AButton type="dashed" block size="small" @click="addRow('bodyParams')">
            <PlusOutlined /> 添加 Body 参数
          </AButton>
        </div>
      </ACollapsePanel>
    </ACollapse>
  </div>
</template>

<style scoped lang="scss">
.extend-config-editor {
  .fixed-system-message-row {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-sm);
    margin-bottom: var(--spacing-md);

    .label-wrap {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .label {
        font-size: 14px;
        color: var(--color-text-primary);
      }
    }
  }

  :deep(.ant-input, .ant-input-select) {
    background-color: white !important;
  }
  :deep(.ant-select-selector) {
    background-color: white !important;
  }
  .param-list {
    .param-row {
      display: flex;
      gap: var(--spacing-sm);
      align-items: center;
      margin-bottom: var(--spacing-sm);

      .param-key {
        flex: 1;
        min-width: 120px;
      }

      .param-value {
        flex: 1;
        min-width: 140px;
      }

      .param-type {
        width: 90px;
        flex-shrink: 0;
      }

      .param-remove {
        flex-shrink: 0;
      }
    }

    .param-row-body {
      .param-key {
        flex: 0.8;
      }

      .param-value {
        flex: 1.2;
      }
    }
  }

  &.compact {
    :deep(.ant-collapse-header) {
      padding: 8px !important;
    }

    .param-row {
      margin-bottom: 6px;
    }
  }
}
</style>
