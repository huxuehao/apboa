<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import {
  FolderOutlined
} from '@ant-design/icons-vue'
import type { FlatFileItem } from '@/composables/chat/useWorkspaceFiles'
import FileIcon from "@/components/workspace/FileIcon.vue";

const props = defineProps<{
  /** 是否显示 */
  visible: boolean
  /** 扁平文件列表 */
  items: FlatFileItem[]
  /** 过滤关键词 */
  keyword?: string
}>()

const emit = defineEmits<{
  (e: 'select', item: FlatFileItem): void
  (e: 'close'): void
}>()

const activeIndex = ref(0)
const listRef = ref<HTMLDivElement | null>(null)
const itemRefs = ref<HTMLDivElement[]>([])

/**
 * 根据关键词过滤后的文件列表
 */
const filteredItems = computed(() => {
  const kw = (props.keyword || '').trim().toLowerCase()
  if (!kw) return props.items
  return props.items.filter(
    (item) =>
      item.name.toLowerCase().includes(kw) ||
      item.fullName.toLowerCase().includes(kw)
  )
})

/**
 * 选中当前高亮的文件
 */
const confirmSelection = () => {
  const items = filteredItems.value
  if (items.length === 0) {
    emit('close')
    return
  }
  const idx = Math.max(0, Math.min(activeIndex.value, items.length - 1))
  emit('select', items[idx]!)
}

/**
 * 处理键盘导航
 */
const handleKeydown = (e: KeyboardEvent) => {
  if (!props.visible) return

  const items = filteredItems.value
  if (items.length === 0) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % items.length
    scrollToActive()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + items.length) % items.length
    scrollToActive()
  } else if (e.key === 'Enter') {
    e.preventDefault()
    confirmSelection()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    emit('close')
  }
}

/**
 * 滚动到当前高亮项
 */
const scrollToActive = () => {
  nextTick(() => {
    const el = itemRefs.value[activeIndex.value]
    if (el && listRef.value) {
      el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  })
}

/**
 * 处理点击选中
 */
const handleItemClick = (item: FlatFileItem) => {
  emit('select', item)
}

// 当过滤条件变化时，重置选中索引
watch(
  () => props.keyword,
  () => {
    activeIndex.value = 0
  }
)

// 当显示状态变化时，重置选中索引
watch(
  () => props.visible,
  (val) => {
    if (val) {
      activeIndex.value = 0
    }
  }
)

// 暴露键盘处理方法供父组件调用
defineExpose({
  handleKeydown
})
</script>

<template>
  <Transition name="mention-dropdown">
    <div
      v-if="visible"
      ref="listRef"
      class="resource-mention-dropdown"
      @mousedown.prevent
    >
      <div v-if="filteredItems.length === 0" class="mention-dropdown-empty">
        <FolderOutlined />
        <span>未找到匹配的文件</span>
      </div>
      <div
        v-for="(item, index) in filteredItems"
        :key="item.path"
        :ref="(el) => { if (el) itemRefs[index] = el as HTMLDivElement }"
        class="mention-dropdown-item"
        :class="{ active: index === activeIndex }"
        @click="handleItemClick(item)"
        @mouseenter="activeIndex = index"
      >
        <span class="mention-dropdown-item-icon">
          <FileIcon :file-name="item.fullName" width="18" />
        </span>
        <div class="mention-dropdown-item-content">
            <span class="mention-dropdown-item-name" :title="item.fullName">
              {{ item.fullName }}
            </span>
          <span
            v-if="item.folderPath"
            class="mention-dropdown-item-folder"
            :title="item.folderPath"
          >
              {{ item.folderPath }}
            </span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.resource-mention-dropdown {
  position: absolute;
  z-index: 100;
  bottom: calc(100% + 12px);
  left: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 -5px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid var(--color-border-light);
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
  padding: 6px;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border-light) transparent;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: var(--color-border-light);
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: var(--color-text-placeholder);
  }
}

.mention-dropdown-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  color: var(--color-text-placeholder);
  font-size: 14px;
}

.mention-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  min-width: 0;

  &:hover,
  &.active {
    background-color: rgba(15, 116, 255, 0.08);
  }
}

.mention-dropdown-item-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
}

.mention-dropdown-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mention-dropdown-item-name {
  font-size: 14px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
}

.mention-dropdown-item-folder {
  font-size: 12px;
  color: var(--color-text-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: 50%;

  /* 在空间不足时优先保证文件名显示，文件夹路径可以被截断 */
  @media (max-width: 400px) {
    max-width: 50%;
  }
}

.mention-dropdown-enter-active,
.mention-dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.mention-dropdown-enter-from,
.mention-dropdown-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
