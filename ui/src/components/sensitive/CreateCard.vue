/**
 * 新增敏感词配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'

/**
 * Emits定义
 */
defineEmits<{
  click: []
}>()
</script>

<template>
  <div class="create-card flex-col flex-center cursor-pointer" @click="$emit('click')">
    <PlusOutlined class="create-icon" />
    <div class="create-text text-secondary">添加新敏感词配置</div>
    <div class="create-desc text-placeholder text-sm">配置敏感词规则用于内容审核</div>
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
}
</style>
