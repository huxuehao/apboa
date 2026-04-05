/**
 * A2A增强配置
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import Sortable from 'sortablejs'
import { HolderOutlined } from '@ant-design/icons-vue'
import * as hookApi from '@/api/hook'
import type { ToolVO, HookConfigVO } from '@/types'
import { countCommonElements } from '@/utils/tools'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    hook: string[]
    enableMemory: boolean
  }
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
}>()

const formRef = ref()
const loading = ref(false)

const allTools = ref<ToolVO[]>([])
const allHooks = ref<HookConfigVO[]>([])

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})



/**
 * 按类型分组的钩子（内置、自定义）
 */
const hooksByType = computed(() => {
  const builtin = allHooks.value.filter(h => h.hookType === 'BUILTIN')
  const custom = allHooks.value.filter(h => h.hookType === 'CUSTOM')
  return { '内置': builtin, '自定义': custom } as Record<string, HookConfigVO[]>
})

/**
 * 已选择的钩子列表（保持选择顺序，用于拖拽排序展示）
 */
const selectedHooks = computed(() => {
  const idToHook = new Map(allHooks.value.map(h => [h.id, h]))
  return formData.value.hook
    .map(id => idToHook.get(id))
    .filter((h): h is HookConfigVO => !!h)
})

const selectedHooksListRef = ref<HTMLElement | null>(null)
let sortableInstance: ReturnType<typeof Sortable.create> | null = null

/**
 * 初始化钩子列表拖拽排序
 */
function initHookSortable() {
  if (!selectedHooksListRef.value || selectedHooks.value.length === 0) return
  if (sortableInstance) return

  sortableInstance = Sortable.create(selectedHooksListRef.value, {
    animation: 150,
    handle: '.hook-drag-handle',
    ghostClass: 'hook-sortable-ghost',
    chosenClass: 'hook-sortable-chosen',
    dragClass: 'hook-sortable-drag',
    forceFallback: false,
    fallbackClass: 'hook-sortable-fallback',
    preventOnFilter: false,
    onStart: () => {
      document.body.classList.add('dragging')
    },
    onEnd: async (evt: { oldIndex?: number; newIndex?: number }) => {
      document.body.classList.remove('dragging')
      const { oldIndex, newIndex } = evt
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return
      const newOrder = [...formData.value.hook]
      const [item] = newOrder.splice(oldIndex, 1)
      if (item === undefined) return
      newOrder.splice(newIndex, 0, item)
      formData.value = { ...formData.value, hook: newOrder }
      // 拖拽后销毁并重新初始化 Sortable，确保 DOM 与 Vue 数据同步
      destroyHookSortable()
      await nextTick()
      initHookSortable()
    }
  })
}

/**
 * 销毁拖拽实例
 */
function destroyHookSortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

/**
 * 加载所有钩子
 */
async function loadAllHooks() {
  const response = await hookApi.page({ page: 1, size: 1000, enabled: true })
  allHooks.value = response.data.data.records || []
}

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}


const handleHookChange = (hookId: string, checked: boolean) => {
  if (checked) {
    formData.value.hook.push(hookId);
  } else {
    const index = formData.value.hook.indexOf(hookId);
    if (index > -1) {
      formData.value.hook.splice(index, 1);
    }
  }
};

watch(
  () => selectedHooks.value.length,
  async (len) => {
    destroyHookSortable()
    if (len > 0) {
      await nextTick()
      initHookSortable()
    }
  }
)

onMounted(async () => {
  await loadAllHooks()
  if (selectedHooks.value.length > 0) {
    await nextTick()
    initHookSortable()
  }
})

onUnmounted(() => {
  destroyHookSortable()
})

defineExpose({
  validate
})
</script>

<template>
  <ASpin :spinning="loading">
    <AForm ref="formRef" :model="formData" layout="vertical">
      <AFormItem label="启用记忆持久化">
        <ASwitch v-model:checked="formData.enableMemory" />
        <div class="text-placeholder text-xs mt-xs">
          开启后,智能体将能够记住对话历史并持久化到数据库
        </div>
      </AFormItem>
      <AFormItem label="钩子配置">
        <ACollapse>
          <ACollapsePanel
            v-for="(hooks, typeLabel) in hooksByType"
            :key="typeLabel"
            :header="`${typeLabel}（${countCommonElements(hooks.map(i => i.id), formData.hook)}/${hooks.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="hook in hooks"
                :checked="formData.hook.includes(hook.id)"
                @change="(e: any) => handleHookChange(hook.id, e.target.checked)"
                :key="hook.id"
                :value="hook.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="hook.name">{{ hook.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="hook.description">{{ hook.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>

        <div v-if="selectedHooks.length > 0" class="selected-hooks-section mt-md">
          <div class="selected-hooks-label text-secondary text-sm mb-sm">
            已选钩子（共 {{ selectedHooks.length }} 个，拖拽调整执行顺序）
          </div>
          <div ref="selectedHooksListRef" class="selected-hooks-list">
            <div
              v-for="hook in selectedHooks"
              :key="hook.id"
              class="selected-hook-item"
            >
              <span class="hook-drag-handle" title="拖拽排序">
                <HolderOutlined class="hook-drag-handle-icon" />
              </span>
              <div class="selected-hook-info">
                <span class="selected-hook-name" :title="hook.name">{{ hook.name }}</span>
                <span class="selected-hook-type" :class="hook.hookType === 'BUILTIN' ? 'type-builtin' : 'type-custom'">
                  {{ hook.hookType === 'BUILTIN' ? '内置' : '自定义' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </AFormItem>
    </AForm>
  </ASpin>
</template>

<style scoped lang="scss">
.checkbox-grid,
.radio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-sm);
}

.checkbox-item,
.radio-item {
  padding: var(--spacing-sm);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  margin: 0 !important;
  transition: all var(--transition-base);
  width: 300px;

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.item-info {
  .item-name {
    font-weight: 500;
    margin-bottom: 4px;
    width:250px;
  }

  .item-desc {
    line-height: 1.4;
  }
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  width:250px;
}

.selected-hooks-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  border: 1px solid var(--color-border-base);
}

.selected-hooks-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.selected-hook-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--color-bg-white);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  transition: all var(--transition-base);
  cursor: default;

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.hook-drag-handle {
  display: flex;
  align-items: center;
  cursor: grab;
  padding: 2px;

  &:active {
    cursor: grabbing;
  }

  .hook-drag-handle-icon {
    color: #999;
    font-size: 16px;

    &:hover {
      color: #666;
    }
  }
}

.selected-hook-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.selected-hook-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-hook-type {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--border-radius-sm);

  &.type-builtin {
    background-color: #e3f2fd;
    color: #1976d2;
  }

  &.type-custom {
    background-color: #fde6f6;
    color: #db2781;
  }
}

:deep(.hook-sortable-ghost) {
  opacity: 0.5;
  background-color: #f0f0f0;
  border: 1px dashed #1890ff;
}

:deep(.hook-sortable-chosen) {
  background-color: #fafafa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.hook-sortable-drag) {
  opacity: 0.9;
  transform: rotate(2deg);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

:deep(.hook-sortable-fallback) {
  opacity: 1 !important;
}

/* 防止拖拽时页面滚动 */
:global(body.dragging) {
  user-select: none;
  -webkit-user-select: none;
}
</style>
