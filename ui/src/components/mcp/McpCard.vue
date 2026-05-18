/**
 * MCP 服务配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import {
  CheckCircleOutlined,
  CloudServerOutlined,
  EllipsisOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import type { McpServerVO } from '@/types'
import { McpActivationStatus } from '@/types'
import {
  createActivateItem,
  createDeleteItem,
  createDivider,
  createEditItem,
  createEnableItem,
  createSyncItem,
  createToolGovernanceItem,
  createViewItem
} from '@/composables/useCardMenuItems'
import {
  getMcpConnectionStatusColor,
  getMcpConnectionStatusText,
  getMcpPrimaryAction
} from '@/composables/useMcpPresentation'

const props = defineProps<{
  data: McpServerVO
}>()

const emit = defineEmits<{
  view: [id: string]
  edit: [id: string]
  delete: [id: string]
  enable: [id: string]
  activate: [id: string]
  sync: [id: string]
  toolGovernance: [id: string]
}>()

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

const modeText = computed(() => {
  return props.data.mode === 'SYNC' ? '同步' : '异步'
})

const connectionText = computed(() => getMcpConnectionStatusText(props.data))

const connectionColor = computed(() => getMcpConnectionStatusColor(props.data))

const primaryAction = computed(() => getMcpPrimaryAction(props.data))

const menuItems = computed(() => {
  const items = [
    createViewItem(),
    createEditItem()
  ]

  if (primaryAction.value?.key === 'activate') {
    items.push(createActivateItem(primaryAction.value.label))
  } else if (primaryAction.value?.key === 'sync') {
    items.push(createSyncItem(primaryAction.value.label))
  }

  items.push(
    createToolGovernanceItem(),
    createEnableItem(props.data.enabled),
    createDivider(),
    createDeleteItem()
  )

  return items
})

function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'view':
      emit('view', props.data.id as string)
      break
    case 'edit':
      emit('edit', props.data.id as string)
      break
    case 'activate':
      emit('activate', props.data.id as string)
      break
    case 'sync':
      emit('sync', props.data.id as string)
      break
    case 'toolGovernance':
      emit('toolGovernance', props.data.id as string)
      break
    case 'enable':
      emit('enable', props.data.id as string)
      break
    case 'delete':
      emit('delete', props.data.id as string)
      break
  }
}
</script>

<template>
  <div class="mcp-card">
    <div class="card-header flex items-center gap-sm">
      <div class="card-avatar flex-center" :class="{ disabled: !data.enabled }">
        <CloudServerOutlined />
      </div>
      <div class="card-name flex-1 truncate" :title="data.name" @click="emit('view', data.id as string)">
        {{ data.name }}
      </div>
      <ADropdown :trigger="['hover']">
        <AButton type="text" size="small" v-permission="['EDIT','ADMIN']">
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

    <div class="card-status flex items-center gap-xs">
      <ATag :color="connectionColor" class="tag">{{ connectionText }}</ATag>
      <ATag v-if="data.needsSync" color="warning" class="tag">待刷新</ATag>
      <ATag color="default" class="tag">工具 {{ data.toolCount || 0 }}</ATag>
      <ATag color="processing" class="tag">全局可用 {{ data.availableToolCount || 0 }}</ATag>
    </div>

    <div class="card-footer flex items-center justify-between">
      <div class="card-tags flex items-center gap-xs">
        <ATag color="default" class="tag">{{ data.protocol }}</ATag>
        <ATag color="default" class="tag">{{ modeText }}</ATag>
      </div>
      <div class="card-time text-placeholder text-xs">更新于 {{ formattedTime }}</div>
    </div>

    <div class="card-hint text-placeholder text-xs">
      <template v-if="data.activationStatus === McpActivationStatus.ACTIVATING">
        <LoadingOutlined style="margin-right: 4px" />{{ data.activationMessage || '正在连接 MCP 并刷新工具目录' }}
      </template>
      <template v-else-if="data.activationStatus === McpActivationStatus.FAILED">
        <ExclamationCircleOutlined style="margin-right: 4px" />{{ data.activationMessage || '连接失败' }}
      </template>
      <template v-else-if="data.activationStatus === McpActivationStatus.ACTIVE && data.toolCount > 0">
        <CheckCircleOutlined style="margin-right: 4px" />{{ data.activationMessage || '连接可用，工具目录已就绪' }}
      </template>
      <template v-else-if="data.activationStatus === McpActivationStatus.ACTIVE">
        <CheckCircleOutlined style="margin-right: 4px" />{{ data.activationMessage || '连接成功，但当前未发现可用工具' }}
      </template>
      <template v-else>
        {{ data.activationMessage || '保存后可手动连接' }}
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mcp-card {
  min-height: 220px;
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
      color: #66bb6a;
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

  .card-status {
    flex-wrap: wrap;
  }

  .card-footer {
    padding-top: var(--spacing-xs);

    .card-tags {
      flex-wrap: wrap;
    }

    .card-time {
      white-space: nowrap;
    }
  }

  .card-hint {
    line-height: 1.5;
    min-height: 20px;
  }

  .disabled {
    color: #757575 !important;
    background-color: #e7e7e7 !important;
  }
}
</style>
