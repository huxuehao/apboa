/**
 * 新增智能体卡片组件 — 支持自定义、A2A WellKnown、A2A Nacos 三种类型
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'

/**
 * Props定义
 */
const props = defineProps<{
  agentType: string | null
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'create-custom': []
  'create-a2a': [type: 'WELLKNOWN' | 'NACOS']
}>()

/**
 * 是否显示单一按鈕模式（自定义 tab）
 */
const isCustomOnly = computed(() => props.agentType === 'CUSTOM')

/**
 * A2A 选项列表
 */
const a2aOptions = [
  { value: 'WELLKNOWN' as const, label: 'WellKnown', desc: '基于 well-known/agent-card.json 协议' },
  { value: 'NACOS' as const, label: 'Nacos', desc: '基于 Nacos 实现 Agent 发现' }
]

/**
 * 全部选项列表（全部 tab 下显示）
 */
const allOptions = [
  { value: 'CUSTOM' as const, label: '自定义', desc: '自行配置模型、提示词、工具等能力' },
  ...a2aOptions.map(o => ({ ...o, label: `A2A · ${o.label}` }))
]

/**
 * 当前显示的选项
 */
const displayOptions = computed(() => {
  if (props.agentType === 'A2A') return a2aOptions
  return allOptions
})

/**
 * 处理点击
 */
function handleOptionClick(value: 'CUSTOM' | 'WELLKNOWN' | 'NACOS') {
  if (value === 'CUSTOM') {
    emit('create-custom')
  } else {
    emit('create-a2a', value)
  }
}
</script>

<template>
  <div v-if="isCustomOnly" class="create-card flex-col flex-center cursor-pointer" @click="emit('create-custom')">
    <PlusOutlined class="create-icon" />
    <div class="create-text text-secondary">添加自定义智能体</div>
    <div class="create-desc text-placeholder text-sm">配置模型、提示词、工具等能力</div>
  </div>

  <div v-else class="create-card type-select">
    <div class="type-list flex-col gap-sm">
      <div
        v-for="option in displayOptions"
        :key="option.value"
        class="type-item cursor-pointer"
        @click="handleOptionClick(option.value)"
      >
        <div class="type-label">
          <PlusOutlined class="type-icon"/>&nbsp;
          <span>{{ option.label }}</span>
        </div>
        <div class="type-desc text-placeholder text-xs">{{ option.desc }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.create-card {
  min-height: 180px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-white);
  border: 2px dotted var(--color-border-base);
  border-radius: var(--border-radius-lg);
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);

    .create-icon {
      color: var(--color-primary);
      transform: scale(1.1);
    }
  }

  .create-icon {
    font-size: 32px;
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-sm);
    transition: all var(--transition-base);
  }

  .create-text {
    font-size: var(--font-size-base);
    font-weight: 500;
    margin-bottom: var(--spacing-xs);
  }

  .create-desc {
    text-align: center;
  }

  &.type-select {
    cursor: default;
    justify-content: center;

    &:hover {
      .create-icon {
        transform: none;
      }
    }

    .type-list {
      width: 100%;

      .type-item {
        padding: 3px 8px;
        border-radius: var(--border-radius-base);
        border: 1px solid transparent;
        transition: all var(--transition-base);

        &:hover {
          background-color: #ededed;
        }

        .type-label {
          font-size: var(--font-size-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          margin-bottom: 2px;
        }

        .type-desc {
          line-height: 1.4;
        }
      }
    }
  }
}
</style>
