/**
 * 新增知识库配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { KbType } from '@/types'

/**
 * Props定义
 */
const props = defineProps<{
  kbType: KbType | null
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  create: [kbType: KbType]
}>()

/**
 * 是否显示类型选择
 */
const showTypeSelect = computed(() => props.kbType === null)

/**
 * 类型选项
 */
const typeOptions = [
  { value: 'BAILIAN', label: '百炼', desc: '百炼平台的知识库服务' },
  { value: 'DIFY', label: 'Dify', desc: 'Dify平台的知识库管理服务' },
  { value: 'RAGFLOW', label: 'RAGFlow', desc: 'RagFlow平台的智能检索服务' }
]

/**
 * 当前类型显示文本
 */
const currentTypeText = computed(() => {
  if (props.kbType === 'BAILIAN') return '百炼知识库'
  if (props.kbType === 'DIFY') return 'Dify知识库'
  if (props.kbType === 'RAGFLOW') return 'RAGFlow知识库'
  return ''
})

/**
 * 处理创建
 */
function handleCreate(kbType?: KbType) {
  if (kbType) {
    emit('create', kbType)
  } else if (props.kbType) {
    emit('create', props.kbType)
  }
}
</script>

<template>
  <div v-if="showTypeSelect" class="create-card type-select">
    <div class="type-list flex-col gap-sm">
      <div
        v-for="option in typeOptions"
        :key="option.value"
        class="type-item cursor-pointer"
        @click="handleCreate(option.value as KbType)"
      >
        <div class="type-label"><PlusOutlined /> {{ option.label }}</div>
        <div class="type-desc text-placeholder text-xs">{{ option.desc }}</div>
      </div>
    </div>
  </div>

  <div v-else class="create-card flex-col flex-center cursor-pointer" @click="handleCreate()">
    <PlusOutlined class="create-icon" />
    <div class="create-text text-secondary">添加{{ currentTypeText }}</div>
    <div class="create-desc text-placeholder text-sm">创建新的知识库配置</div>
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
          background-color: #eeeeee;
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
