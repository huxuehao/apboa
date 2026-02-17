/**
 * 新增MCP服务器配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { McpProtocol } from '@/types'

/**
 * Props定义
 */
const props = defineProps<{
  protocol: McpProtocol | null
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  create: [protocol: McpProtocol]
}>()

/**
 * 是否显示协议选择
 */
const showProtocolSelect = computed(() => props.protocol === null)

/**
 * 协议选项
 */
const protocolOptions = [
  { value: 'HTTP', label: 'HTTP', desc: '基于HTTP/HTTPS协议的MCP服务器' },
  { value: 'SSE', label: 'SSE', desc: '基于Server-Sent Events的MCP服务器' },
  { value: 'STDIO', label: 'STDIO', desc: '基于标准输入输出的MCP服务器' }
]

/**
 * 当前协议显示文本
 */
const currentProtocolText = computed(() => {
  if (props.protocol === 'HTTP') return 'HTTP MCP服务器'
  if (props.protocol === 'SSE') return 'SSE MCP服务器'
  if (props.protocol === 'STDIO') return 'STDIO MCP服务器'
  return ''
})

/**
 * 处理创建
 */
function handleCreate(protocol?: McpProtocol) {
  if (protocol) {
    emit('create', protocol)
  } else if (props.protocol) {
    emit('create', props.protocol)
  }
}
</script>

<template>
  <div v-if="showProtocolSelect" class="create-card protocol-select">
<!--    <div class="protocol-header flex-col flex-center">-->
<!--      <PlusOutlined class="create-icon" />-->
<!--      <div class="create-text text-secondary">添加MCP服务器</div>-->
<!--      <div class="create-desc text-placeholder text-sm">选择协议类型创建MCP服务器</div>-->
<!--    </div>-->
    <div class="protocol-list flex-col gap-sm">
      <div
        v-for="option in protocolOptions"
        :key="option.value"
        class="protocol-item cursor-pointer"
        @click="handleCreate(option.value as McpProtocol)"
      >
        <div class="protocol-label"><PlusOutlined /> {{ option.label }}</div>
        <div class="protocol-desc text-placeholder text-xs">{{ option.desc }}</div>
      </div>
    </div>
  </div>

  <div v-else class="create-card flex-col flex-center cursor-pointer" @click="handleCreate()">
    <PlusOutlined class="create-icon" />
    <div class="create-text text-secondary">添加{{ currentProtocolText }}</div>
    <div class="create-desc text-placeholder text-sm">创建新的MCP服务器配置</div>
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

  &.protocol-select {
    cursor: default;

    &:hover {
      .create-icon {
        transform: none;
      }
    }

    .protocol-header {
      margin-bottom: var(--spacing-md);
      padding-bottom: var(--spacing-md);
      border-bottom: 1px solid var(--color-border-light);
    }

    .protocol-list {
      width: 100%;

      .protocol-item {
        padding: var(--spacing-xs);
        //background-color: var(--color-bg-light);
        border-radius: var(--border-radius-base);
        border: 1px solid transparent;
        transition: all var(--transition-base);

        &:hover {
          background-color: var(--color-primary-light);
          border: 1px solid var(--color-border-base);
          //transform: translateX(4px);
        }

        .protocol-label {
          font-size: var(--font-size-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          margin-bottom: 2px;
        }

        .protocol-desc {
          line-height: 1.4;
        }
      }
    }
  }
}
</style>
