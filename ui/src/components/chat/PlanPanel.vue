<script setup lang="ts">
/**
 * 计划面板组件
 *
 * 实时展示 Agent 计划执行状态，支持折叠/展开，
 * 计划完成或对话中断时自动销毁。
 *
 * @author huxuehao
 */
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import {
  LoadingOutlined,
  CheckCircleFilled,
  MinusCircleFilled,
  CloseCircleFilled,
  DownOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
import type { PlanInfo, SubTaskInfo } from '@/types'

const props = defineProps<{
  plan: PlanInfo
  isRunning: boolean
}>()

const emit = defineEmits<{
  (e: 'destroy'): void
}>()

/** 展开/折叠状态 */
const isExpanded = ref(false)

/** 是否有子任务正在进行中 */
const hasInProgress = computed(() =>
  props.plan.subtasks.some((t) => t.state === 'in_progress')
)

/** 非终态子任务数量（todo + in_progress） */
const activeCount = computed(
  () =>
    props.plan.subtasks.filter(
      (t) => t.state === 'todo' || t.state === 'in_progress'
    ).length
)

/** 销毁定时器 */
let destroyTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 获取子任务状态对应的图标组件
 */
function getSubtaskIcon(state: SubTaskInfo['state']) {
  switch (state) {
    case 'in_progress':
      return LoadingOutlined
    case 'done':
      return CheckCircleFilled
    case 'removed':
      return MinusCircleFilled
    case 'abandoned':
      return CloseCircleFilled
    default:
      return null
  }
}

/**
 * 获取子任务行的 CSS class
 */
function getSubtaskClass(state: SubTaskInfo['state']) {
  return {
    'plan-subtask': true,
    'plan-subtask--removed': state === 'removed',
    'plan-subtask--abandoned': state === 'abandoned',
    'plan-subtask--done': state === 'done'
  }
}

/**
 * 切换展开/折叠
 */
function toggleExpand(): void {
  isExpanded.value = !isExpanded.value
}

/**
 * 调度自动销毁
 */
function scheduleDestroy(): void {
  if (destroyTimer) return
  destroyTimer = setTimeout(() => {
    emit('destroy')
  }, 2000)
}

/**
 * 取消自动销毁
 */
function cancelDestroy(): void {
  if (destroyTimer) {
    clearTimeout(destroyTimer)
    destroyTimer = null
  }
}

/**
 * 监听计划变化，当所有子任务均为终态时自动销毁
 */
watch(
  () => props.plan.subtasks,
  (subtasks) => {
    if (!subtasks.length) return

    const allTerminal = subtasks.every((t) =>
      ['done', 'abandoned', 'removed'].includes(t.state)
    )

    if (allTerminal) {
      scheduleDestroy()
    } else {
      cancelDestroy()
    }
  },
  { deep: true, immediate: true }
)

onBeforeUnmount(() => {
  cancelDestroy()
})
</script>

<template>
  <div class="plan-panel">
    <!-- 折叠头部 -->
    <div class="plan-panel-header" @click="toggleExpand">
      <div class="plan-panel-header-left">
        <LoadingOutlined
          v-if="hasInProgress"
          class="plan-panel-spinner"
          spin
        />
        <span class="plan-panel-name" :title="plan.name">{{ plan.name }}</span>
      </div>
      <div class="plan-panel-header-right">
        <span class="plan-panel-count">
          {{ plan.subtasks.length - activeCount }}/{{ plan.subtasks.length }} Tasks
        </span>
        <span class="plan-panel-arrow">
          <DownOutlined v-if="isExpanded" />
          <RightOutlined v-else />
        </span>
      </div>
    </div>

    <!-- 展开的子任务列表 -->
    <Transition name="plan-collapse">
      <div v-if="isExpanded" class="plan-panel-list">
        <div
          v-for="(subtask, idx) in plan.subtasks"
          :key="idx"
          :class="getSubtaskClass(subtask.state)"
        >
          <span class="plan-subtask-icon">
            <component
              :is="getSubtaskIcon(subtask.state)"
              v-if="getSubtaskIcon(subtask.state)"
            />
            <span v-else class="plan-subtask-dot" />
          </span>
          <span class="plan-subtask-name" :title="subtask.name">
            {{ subtask.name }}
          </span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;

.plan-panel {
  position: sticky;
  top: 0;
  z-index: 10;
  max-width: 800px;
  margin: 0 auto var(--spacing-sm);
  background-color: $chat-bg-main;
  border: 1px solid var(--color-border-extra-light);
  border-radius: var(--border-radius-lg);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  width: calc(100% - 16px);
}

.plan-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-sm) var(--spacing-base);
  cursor: pointer;
  user-select: none;
  transition: background-color $chat-transition;
  min-height: 40px;
  gap: var(--spacing-sm);

  &:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }
}

.plan-panel-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
  flex: 1;
}

.plan-panel-header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}

.plan-panel-spinner {
  font-size: 14px;
  color: $chat-primary;
  flex-shrink: 0;
  animation: plan-spin 1s linear infinite;

  @keyframes plan-spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
}

.plan-panel-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-panel-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
  white-space: nowrap;
}

.plan-panel-arrow {
  font-size: 12px;
  color: var(--color-text-placeholder);
  transition: transform 0.2s ease;
  display: flex;
  align-items: center;
}

.plan-panel-list {
  border-top: 1px solid var(--color-border-extra-light);
  padding: var(--spacing-xs) 0;
  overflow: hidden;
}

.plan-subtask {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px var(--spacing-base);
  font-size: var(--font-size-sm);
  color: var(--color-text-regular);
  transition: background-color 0.15s ease;

  &:hover {
    background-color: rgba(0, 0, 0, 0.02);
  }

  &--done,
  &--removed,
  &--abandoned {
    opacity: 0.5;
  }
}

.plan-subtask-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  font-size: 14px;

  :deep(.anticon-check-circle) {
    color: #52c41a;
  }

  :deep(.anticon-loading) {
    color: $chat-primary;
  }

  :deep(.anticon-minus-circle),
  :deep(.anticon-close-circle) {
    color: var(--color-text-placeholder);
  }
}

.plan-subtask-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--color-text-placeholder);
  opacity: 0.6;
}

.plan-subtask-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 折叠/展开过渡动画 */
.plan-collapse-enter-active,
.plan-collapse-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.plan-collapse-enter-from,
.plan-collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.plan-collapse-enter-to,
.plan-collapse-leave-from {
  opacity: 1;
  max-height: 2000px;
}
</style>
