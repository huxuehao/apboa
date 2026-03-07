/**
 * A2A WellKnown 协议配置表单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { WellKnownAgentConfig } from '@/types'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: WellKnownAgentConfig
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: WellKnownAgentConfig]
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
 * 表单验证规则
 */
const rules = {
  baseUrl: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' },
    { type: 'url', message: '请输入有效的 URL 地址', trigger: 'blur' }
  ],
  relativeCardPath: [
    { required: true, message: '请输入 Agent Card 路径', trigger: 'blur' }
  ]
}

/**
 * 添加认证头
 */
function addAuthHeader() {
  formData.value.authHeaders.push({ key: '', value: '', evn: false })
}

/**
 * 删除认证头
 *
 * @param index 行索引
 */
function removeAuthHeader(index: number) {
  formData.value.authHeaders.splice(index, 1)
}

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    // 校验 authHeaders 中的 key
    const hasEmptyKey = formData.value.authHeaders.some(h => !h.key?.trim())
    if (hasEmptyKey) {
      return false
    }
    return true
  } catch {
    return false
  }
}

/**
 * 获取表单数据
 */
function getFormData(): WellKnownAgentConfig {
  return { ...formData.value }
}

defineExpose({ validate, getFormData })
</script>

<template>
  <AForm
    ref="formRef"
    :model="formData"
    :rules="rules"
    layout="vertical"
  >
    <AFormItem label="Base URL" name="baseUrl">
      <AInput
        v-model:value="formData.baseUrl"
        placeholder="如: https://your-agent-service.com"
      />
    </AFormItem>

    <AFormItem label="Agent Card 路径" name="relativeCardPath">
      <AInput
        v-model:value="formData.relativeCardPath"
        placeholder="默认: /.well-known/agent-card.json"
      />
    </AFormItem>

    <AFormItem label="认证头（Auth Headers）">
      <div class="kv-list flex-col gap-xs">
        <div
          v-for="(header, index) in formData.authHeaders"
          :key="index"
          class="kv-row flex items-center gap-sm"
        >
          <AInput
            v-model:value="header.key"
            placeholder="Header 名称"
            class="kv-key"
          />
          <div class="kv-env flex items-center gap-xs">
            <span class="text-secondary" style="margin-right: 5px">ENV</span>
            <ASwitch v-model:checked="header.evn" />
          </div>
          <AInput
            v-model:value="header.value"
            :placeholder="`Header 值${header.evn ? '（环境变量）':''}`"
            class="kv-value"
          />
          <div v-if="header.evn" class="kv-env-hint text-xs text-placeholder">
            从环境变量中读取
          </div>
          <AButton
            type="text"
            danger
            size="small"
            @click="removeAuthHeader(index)"
          >
            <DeleteOutlined />
          </AButton>
        </div>

        <AButton type="dashed" class="add-btn" @click="addAuthHeader">
          <PlusOutlined /> 添加认证头
        </AButton>
      </div>
    </AFormItem>
  </AForm>
</template>

<style scoped lang="scss">
.kv-list {
  .kv-row {
    margin-bottom: 10px;
    .kv-key {
      width: 250px;
      flex-shrink: 0;
    }

    .kv-env {
      flex-shrink: 0;
    }

    .kv-value {
      flex: 1;
      min-width: 0;
    }

    .kv-env-hint {
      line-height: 32px;
      padding: 0 8px;
    }
  }

  .add-btn {
    width: 100%;
    margin-top: var(--spacing-xs);
  }
}
</style>
