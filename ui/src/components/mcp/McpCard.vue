/**
 * MCP服务器配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { EllipsisOutlined, CloudServerOutlined } from '@ant-design/icons-vue'
import type { McpServerVO } from '@/types'
import {
  createViewItem,
  createEditItem,
  createEnableItem,
  createDeleteItem,
  createDivider,
} from '@/composables/useCardMenuItems'

/**
 * Props定义
 */
const props = defineProps<{
  data: McpServerVO
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  view: [id: string]
  edit: [id: string]
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
 * 协议类型显示文本
 */
const protocolText = computed(() => {
  return props.data.protocol
})

/**
 * 运行模式显示文本
 */
const modeText = computed(() => {
  return props.data.mode === 'SYNC' ? '同步' : '异步'
})

/**
 * 操作菜单项
 */
const menuItems = computed(() => [
  createViewItem(),
  createEditItem(),
  createEnableItem(props.data.enabled),
  createDivider(),
  createDeleteItem(),
])

/**
 * 处理菜单点击
 */
function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'view':
      emit('view', props.data.id as string)
      break
    case 'edit':
      emit('edit', props.data.id as string)
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
      <div class="card-avatar flex-center" :class="{ disabled: !data.enabled }"><CloudServerOutlined /></div>
      <div class="card-name flex-1 truncate" :title="data.name" @click="emit('view', data.id as string)">{{ data.name }}</div>
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

    <div class="card-footer flex items-center justify-between">
      <div class="card-tags flex items-center gap-xs">
        <ATag color="default" class="tag">{{ protocolText }}</ATag>
        <ATag color="default" class="tag">{{ modeText }}</ATag>
      </div>
      <div class="card-time text-placeholder text-xs">更新于 {{ formattedTime }}</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mcp-card {
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

    .card-tags {
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
