/**
 * 模型供应商卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { EllipsisOutlined, ApiOutlined } from '@ant-design/icons-vue'
import type { ModelProviderVO } from '@/types'
import { useAccountStore } from '@/stores'
import {
  createViewItem,
  createEditItem,
  createEnableItem,
  createDeleteItem,
  createConfigItem,
  createDivider,
} from '@/composables/useCardMenuItems'

const accountStore = useAccountStore()

/**
 * Props定义
 */
const props = defineProps<{
  data: ModelProviderVO
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  view: [id: string]
  edit: [id: string]
  configModels: [id: string, name: string]
  delete: [id: string]
  enable: [id: string]
}>()

/**
 * 格式化更新时间
 */
const formattedTime = computed(() => {
  if (!props.data.updatedAt) return ''
  const date = new Date(props.data.updatedAt)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

/**
 * 供应商类型显示文本
 */
const providerTypeLabels: Record<string, string> = {
  DASH_SCOPE: 'DashScope',
  OPEN_AI: 'OpenAI',
  ANTHROPIC: 'Anthropic',
  GEMINI: 'Gemini',
  OLLAMA: 'Ollama'
}

const providerTypeText = computed(() => {
  return providerTypeLabels[props.data.type] || props.data.type
})

/**
 * 操作菜单项
 */
const hasReadOnly = accountStore.roles.some(role => ['READ_ONLY'].includes(role))
const menuItems = computed(() => {
  if (hasReadOnly) {
    return [
      createViewItem(),
      createDivider(),
      createConfigItem(),
    ]
  }
  return [
    createViewItem(),
    createEditItem(),
    createEnableItem(props.data.enabled),
    createDivider(),
    createConfigItem(),
    createDivider(),
    createDeleteItem(),
  ]
})

/**
 * 处理菜单点击
 */
function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'view':
      emit('view', props.data.id)
      break
    case 'edit':
      emit('edit', props.data.id)
      break
    case 'enable':
      emit('enable', props.data.id)
      break
    case 'config':
      emit('configModels', props.data.id, props.data.name)
      break
    case 'delete':
      emit('delete', props.data.id)
      break
  }
}

/**
 * 处理配置模型按钮点击
 */
function handleConfigClick() {
  emit('configModels', props.data.id, props.data.name)
}
</script>

<template>
  <div class="provider-card">
    <div class="card-header flex items-center gap-sm">
      <div class="card-avatar flex-center" :class="{ disabled: !data.enabled }"><ApiOutlined /></div>
      <div class="card-name flex-1 truncate" :title="data.name" @click="emit('edit', data.id)">{{ data.name }}</div>
      <ADropdown :trigger="['hover']">
        <AButton type="text" size="small">
          <EllipsisOutlined />
        </AButton>
        <template #overlay>
          <AMenu @click="handleMenuClick" :items="menuItems"></AMenu>
        </template>
      </ADropdown>
    </div>

    <div class="card-content line-clamp-3" :title="data.description">
      {{ data.description }}
    </div>

    <div class="card-footer flex items-center justify-between">
      <div class="card-actions flex items-center gap-xs">
        <ATag color="default" class="tag">{{ providerTypeText }}</ATag>
      </div>
      <div class="card-time text-placeholder text-xs">
        更新于 {{ formattedTime }}
      </div>
<!--      <div class="card-time text-placeholder text-xs">-->
<!--        <AButton type="link" size="small" @click="handleConfigClick">配置模型</AButton>-->
<!--      </div>-->
    </div>
  </div>
</template>

<style scoped lang="scss">
.provider-card {
  min-height: 180px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-white);
  border-radius: var(--border-radius-lg);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  transition: all var(--transition-base);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .card-header {
    .card-avatar {
      width: 40px;
      height: 40px;
      background-color: #e8f5e9;
      color: #00b81b;
      border-radius: var(--border-radius-xl);
      font-size: var(--font-size-2xl);
      font-weight: 600;
      flex-shrink: 0;
    }

    .card-name {
      font-size: var(--font-size-base);
      font-weight: 600;
      color: var(--color-text-primary);
      cursor: pointer;
      transition: color var(--transition-base);

      //&:hover {
      //  color: #66bb6a;
      //}
    }
  }

  .card-content {
    font-size: var(--font-size-sm);
    color: var(--color-text-regular);
    line-height: 1.6;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-all;
    min-height: 65px;
    max-height: 65px;
  }

  .card-footer {
    padding-top: var(--spacing-xs);

    .card-actions {
      flex-wrap: wrap;
    }

    .card-time {
      white-space: nowrap;
    }
  }

  .disabled {
    color: #757575 !important;
    background-color: #e7e7e7 !important;
  }
}
</style>
