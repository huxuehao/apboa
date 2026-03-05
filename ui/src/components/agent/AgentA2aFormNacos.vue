/**
 * A2A Nacos 协议配置表单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { NacosAgentConfig } from '@/types'

/**
 * 不可删除的固定 Nacos 属性 key
 */
const FIXED_KEYS = ['serverAddr']

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: NacosAgentConfig
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: NacosAgentConfig]
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
 * 判断某行是否为固定行（不可删除）
 *
 * @param key 属性 key
 */
function isFixed(key: string): boolean {
  return FIXED_KEYS.includes(key)
}

/**
 * 添加自定义属性
 */
function addProperty() {
  formData.value.nacosProperties.push({ key: '', value: '', evn: false })
}

/**
 * 删除自定义属性
 *
 * @param index 行索引
 */
function removeProperty(index: number) {
  const item = formData.value.nacosProperties[index]
  if (!item || isFixed(item.key)) return
  formData.value.nacosProperties.splice(index, 1)
}

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    const hasEmptyKey = formData.value.nacosProperties
      .filter(p => !isFixed(p.key))
      .some(p => !p.key?.trim())
    if (hasEmptyKey) return false
    return true
  } catch {
    return false
  }
}

/**
 * 获取表单数据
 */
function getFormData(): NacosAgentConfig {
  return { ...formData.value }
}

defineExpose({ validate, getFormData })
</script>

<template>
  <AForm
    ref="formRef"
    :model="formData"
    layout="vertical"
  >
    <AFormItem label="Nacos 连接属性">
      <div class="kv-list flex-col gap-xs">
        <div
          v-for="(prop, index) in formData.nacosProperties"
          :key="index"
          class="kv-row flex items-center gap-sm"
        >
          <AInput
            v-model:value="prop.key"
            placeholder="属性名"
            :disabled="isFixed(prop.key)"
            class="kv-key"
          />
          <div class="kv-env flex items-center gap-xs">
            <span class="text-secondary" style="margin-right: 5px">ENV</span>
            <ASwitch v-model:checked="prop.evn" :disabled="isFixed(prop.key)" />
          </div>
          <AInput
            v-if="!prop.evn"
            v-model:value="prop.value"
            :placeholder="isFixed(prop.key) ? `${prop.key} 的值` : '属性值'"
            class="kv-value"
          />
          <div v-else class="kv-env-hint text-xs text-placeholder kv-value">
            从环境变量 <strong>{{ prop.key || '(key)' }}</strong> 中读取
          </div>
          <AButton
            :disabled="isFixed(prop.key)"
            type="text"
            danger
            size="small"
            @click="removeProperty(index)"
          >
            <DeleteOutlined />
          </AButton>
        </div>

        <AButton type="dashed" class="add-btn" @click="addProperty">
          <PlusOutlined /> 添加属性
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

    .kv-placeholder {
      width: 24px;
      flex-shrink: 0;
    }
  }

  .add-btn {
    width: 100%;
    margin-top: var(--spacing-xs);
  }
}
</style>
