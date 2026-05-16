<script setup lang="ts">
/**
 * 资源 @mention 下拉组件
 * 支持双页面交互：
 *  - 主页：工作空间文件列表 + 工具/技能"文件夹"入口
 *  - 详情页：进入文件夹后展示对应类目下的资源列表
 * 双页面间通过方向感知的滑动动画切换
 *
 * @component
 */
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import type { FlatFileItem } from '@/composables/chat/useWorkspaceFiles'
import type {
  AgentSkillItem,
  AgentToolItem,
  MentionResourceItem,
  ResourceKind
} from '@/types/chat-mention'
import {
  RESOURCE_CATEGORY_REGISTRY,
  toResourceItem,
  type MainCategorySection,
  useResourceCategories
} from '@/composables/chat/useResourceCategories'

const props = withDefaults(
  defineProps<{
    /** 是否显示 */
    visible: boolean
    /** 工作空间文件列表 */
    workspaceFiles?: FlatFileItem[]
    /** Agent 工具列表 */
    agentTools?: AgentToolItem[]
    /** Agent 技能列表 */
    agentSkills?: AgentSkillItem[]
    /** 过滤关键词（来自 @ 后输入） */
    keyword?: string
  }>(),
  {
    workspaceFiles: () => [],
    agentTools: () => [],
    agentSkills: () => [],
    keyword: ''
  }
)

const emit = defineEmits<{
  (e: 'select', item: MentionResourceItem): void
  (e: 'close'): void
}>()

/**
 * 类目数据：平铺项（工作空间文件）+ 底部文件夹区段
 */
const { flatItems, folderSections } = useResourceCategories({
  workspaceFiles: computed(() => props.workspaceFiles),
  agentTools: computed(() => props.agentTools),
  agentSkills: computed(() => props.agentSkills)
})

/** 当前视图：主页 / 详情页 */
const view = ref<'main' | 'detail'>('main')
/** 当前进入的详情类目 */
const detailKind = ref<ResourceKind | null>(null)
/** 切换方向，控制滑动动画 */
const direction = ref<'forward' | 'back'>('forward')

/** 主页：高亮项索引（覆盖文件项 + 文件夹项） */
const mainActiveIndex = ref(0)
/** 详情页：高亮项索引 */
const detailActiveIndex = ref(0)

const mainListRef = ref<HTMLDivElement | null>(null)
const detailListRef = ref<HTMLDivElement | null>(null)
const mainItemRefs = ref<HTMLDivElement[]>([])
const detailItemRefs = ref<HTMLDivElement[]>([])

/**
 * 主页：关键词过滤后的工作空间文件项
 */
const filteredFlatItems = computed<MentionResourceItem[]>(() => {
  const kw = (props.keyword || '').trim().toLowerCase()
  if (!kw) return flatItems.value
  return flatItems.value.filter((item) => {
    const inName = item.name.toLowerCase().includes(kw)
    const inDesc = (item.description || '').toLowerCase().includes(kw)
    return inName || inDesc
  })
})

/**
 * 主页底部文件夹区段（按注册表 order 升序）
 */
const visibleFolderSections = computed<MainCategorySection[]>(() => folderSections.value)

/**
 * 主页"行项"统一结构（用于键盘导航）
 *  - flat 类型：可直接选中的资源项
 *  - folder 类型：进入详情页的文件夹入口
 */
type MainEntry =
  | { kind: 'flat'; item: MentionResourceItem }
  | { kind: 'folder'; section: MainCategorySection }

const mainEntries = computed<MainEntry[]>(() => {
  const list: MainEntry[] = []
  for (const item of filteredFlatItems.value) {
    list.push({ kind: 'flat', item })
  }
  for (const section of visibleFolderSections.value) {
    list.push({ kind: 'folder', section })
  }
  return list
})

/**
 * 详情页当前类目元数据
 */
const detailMeta = computed(() => {
  if (!detailKind.value) return null
  return RESOURCE_CATEGORY_REGISTRY[detailKind.value]
})

/**
 * 详情页：关键词过滤后的资源项
 */
const filteredDetailItems = computed<MentionResourceItem[]>(() => {
  if (!detailKind.value) return []
  const list = pickRawListByKind(detailKind.value)
  const items = list.map((raw) => toResourceItem(detailKind.value!, raw))
  const kw = (props.keyword || '').trim().toLowerCase()
  if (!kw) return items
  return items.filter((item) => {
    const inName = item.name.toLowerCase().includes(kw)
    const inDesc = (item.description || '').toLowerCase().includes(kw)
    return inName || inDesc
  })
})

/**
 * 按 kind 取出原始数据列表
 */
function pickRawListByKind(
  kind: ResourceKind
): Array<FlatFileItem | AgentToolItem | AgentSkillItem> {
  if (kind === 'workspace-file') return props.workspaceFiles
  if (kind === 'agent-tool') return props.agentTools
  if (kind === 'agent-skill') return props.agentSkills
  return []
}

/**
 * 进入文件夹详情页
 *
 * @param section 类目区段
 */
const enterFolder = (section: MainCategorySection) => {
  direction.value = 'forward'
  detailKind.value = section.kind
  detailActiveIndex.value = 0
  // 在下一帧再切换 view，确保 direction 已经被订阅到 transition name
  nextTick(() => {
    view.value = 'detail'
    scrollToActive('detail')
  })
}

/**
 * 返回主页
 */
const backToMain = () => {
  direction.value = 'back'
  nextTick(() => {
    view.value = 'main'
    scrollToActive('main')
  })
}

/**
 * 主页确认选中
 */
const confirmMainSelection = () => {
  const entries = mainEntries.value
  if (entries.length === 0) {
    emit('close')
    return
  }
  const idx = clampIndex(mainActiveIndex.value, entries.length)
  const entry = entries[idx]
  if (!entry) return
  if (entry.kind === 'flat') {
    emit('select', entry.item)
  } else {
    enterFolder(entry.section)
  }
}

/**
 * 详情页确认选中
 */
const confirmDetailSelection = () => {
  const items = filteredDetailItems.value
  if (items.length === 0) return
  const idx = clampIndex(detailActiveIndex.value, items.length)
  const target = items[idx]
  if (target) emit('select', target)
}

/**
 * 主页点击行项
 */
const handleMainEntryClick = (entry: MainEntry, index: number) => {
  mainActiveIndex.value = index
  if (entry.kind === 'flat') {
    emit('select', entry.item)
  } else {
    enterFolder(entry.section)
  }
}

/**
 * 详情页点击行项
 */
const handleDetailItemClick = (item: MentionResourceItem, index: number) => {
  detailActiveIndex.value = index
  emit('select', item)
}

/**
 * 安全索引
 */
function clampIndex(idx: number, len: number): number {
  if (len === 0) return 0
  return Math.max(0, Math.min(idx, len - 1))
}

/**
 * 滚动到当前高亮项
 */
const scrollToActive = (which: 'main' | 'detail') => {
  nextTick(() => {
    if (which === 'main') {
      const el = mainItemRefs.value[mainActiveIndex.value]
      if (el) el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    } else {
      const el = detailItemRefs.value[detailActiveIndex.value]
      if (el) el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  })
}

/**
 * 处理键盘导航
 *
 * @param e 键盘事件
 */
const handleKeydown = (e: KeyboardEvent) => {
  if (!props.visible) return

  if (view.value === 'main') {
    handleMainKeydown(e)
  } else {
    handleDetailKeydown(e)
  }
}

/**
 * 主页键盘
 */
const handleMainKeydown = (e: KeyboardEvent) => {
  const entries = mainEntries.value
  if (e.key === 'Escape') {
    e.preventDefault()
    emit('close')
    return
  }
  if (entries.length === 0) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    mainActiveIndex.value = (mainActiveIndex.value + 1) % entries.length
    scrollToActive('main')
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    mainActiveIndex.value =
      (mainActiveIndex.value - 1 + entries.length) % entries.length
    scrollToActive('main')
  } else if (e.key === 'Enter') {
    e.preventDefault()
    confirmMainSelection()
  } else if (e.key === 'ArrowRight') {
    // 右箭头：若高亮文件夹则进入
    const idx = clampIndex(mainActiveIndex.value, entries.length)
    const entry = entries[idx]
    if (entry && entry.kind === 'folder') {
      e.preventDefault()
      enterFolder(entry.section)
    }
  }
}

/**
 * 详情页键盘
 */
const handleDetailKeydown = (e: KeyboardEvent) => {
  const items = filteredDetailItems.value
  if (e.key === 'Escape' || e.key === 'ArrowLeft') {
    e.preventDefault()
    backToMain()
    return
  }
  if (items.length === 0) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    detailActiveIndex.value = (detailActiveIndex.value + 1) % items.length
    scrollToActive('detail')
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    detailActiveIndex.value =
      (detailActiveIndex.value - 1 + items.length) % items.length
    scrollToActive('detail')
  } else if (e.key === 'Enter') {
    e.preventDefault()
    confirmDetailSelection()
  }
}

/**
 * 关键词变化重置高亮
 */
watch(
  () => props.keyword,
  () => {
    mainActiveIndex.value = 0
    detailActiveIndex.value = 0
  }
)

/**
 * 显隐变化时重置全部状态
 */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      mainActiveIndex.value = 0
      detailActiveIndex.value = 0
      view.value = 'main'
      detailKind.value = null
    }
  }
)

/**
 * 暴露给父组件
 */
defineExpose({
  handleKeydown
})

/**
 * Transition name 计算（方向感知）
 */
const transitionName = computed(() =>
  direction.value === 'forward' ? 'slide-forward' : 'slide-back'
)
</script>

<template>
  <Transition name="mention-dropdown">
    <div
      v-if="visible"
      class="resource-mention-dropdown"
      @mousedown.prevent
    >
      <div class="dropdown-stack">
        <Transition :name="transitionName">
          <!-- 主页 -->
          <div v-if="view === 'main'" key="main" class="dropdown-page main-page">
            <div ref="mainListRef" class="main-files-area">
              <div
                v-if="filteredFlatItems.length === 0"
                class="dropdown-empty"
              >
                <span>未找到匹配的文件</span>
              </div>
              <div
                v-for="(entry, index) in mainEntries.slice(0, filteredFlatItems.length)"
                :key="`flat-${entry.kind === 'flat' ? entry.item.id : index}`"
                :ref="(el) => { if (el) mainItemRefs[index] = el as HTMLDivElement }"
                class="dropdown-item"
                :class="{ active: index === mainActiveIndex }"
                @click="handleMainEntryClick(entry, index)"
                @mouseenter="mainActiveIndex = index"
              >
                <span class="dropdown-item-icon">
                  <component
                    :is="
                      entry.kind === 'flat'
                        ? RESOURCE_CATEGORY_REGISTRY[entry.item.kind].renderItemIcon(entry.item)
                        : null
                    "
                  />
                </span>
                <div class="dropdown-item-content">
                  <span
                    v-if="entry.kind === 'flat'"
                    class="dropdown-item-name"
                    :title="entry.item.name"
                  >
                    {{ entry.item.name }}
                  </span>
                  <span
                    v-if="entry.kind === 'flat' && entry.item.description"
                    class="dropdown-item-folder"
                    :title="entry.item.description"
                  >
                    {{ entry.item.description }}
                  </span>
                </div>
              </div>
            </div>

            <div v-if="visibleFolderSections.length > 0" class="main-folders-area">
              <div
                v-for="(section, sIdx) in visibleFolderSections"
                :key="`folder-${section.kind}`"
                :ref="(el) => {
                  const idx = filteredFlatItems.length + sIdx
                  if (el) mainItemRefs[idx] = el as HTMLDivElement
                }"
                class="dropdown-folder-item"
                :class="{ active: filteredFlatItems.length + sIdx === mainActiveIndex }"
                @click="enterFolder(section)"
                @mouseenter="mainActiveIndex = filteredFlatItems.length + sIdx"
              >
                <span class="folder-icon">
                  <component :is="section.meta.folderIcon" />
                </span>
                <span class="folder-label">{{ section.meta.label }}</span>
                <span class="folder-count">{{ section.items.length }}</span>
              </div>
            </div>
          </div>

          <!-- 详情页 -->
          <div v-else key="detail" class="dropdown-page detail-page">
            <div class="detail-header">
              <span class="detail-back-btn" @click="backToMain">
                <ArrowLeftOutlined />
              </span>
              <span class="detail-title">
                {{ detailMeta?.label }}
              </span>
              <span class="detail-count">{{ filteredDetailItems.length }}</span>
            </div>
            <div ref="detailListRef" class="detail-list">
              <div
                v-if="filteredDetailItems.length === 0"
                class="dropdown-empty"
              >
                <span>暂无可用项</span>
              </div>
              <div
                v-for="(item, index) in filteredDetailItems"
                :key="`detail-${item.id}`"
                :ref="(el) => { if (el) detailItemRefs[index] = el as HTMLDivElement }"
                class="dropdown-item detail-item"
                :class="{ active: index === detailActiveIndex }"
                @click="handleDetailItemClick(item, index)"
                @mouseenter="detailActiveIndex = index"
              >
                <div class="dropdown-item-content detail-item-content">
                  <span class="detail-item-name" :title="item.name">
                    {{ item.name }}
                  </span>
                  <span
                    v-if="item.description"
                    class="detail-item-desc"
                    :title="item.description"
                  >
                    {{ item.description }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Transition>
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
  width: 100%;
  max-height: 320px;
  overflow: hidden;
  text-align: left;
}

// 使用 grid 单元格叠加，使“当前可见 page”自然擑起容器高度
.dropdown-stack {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: minmax(0, max-content);
  max-height: 320px;
  overflow: hidden;
}

.dropdown-page {
  grid-row: 1;
  grid-column: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-height: 0;
  // 限制 page 高度不超过 stack，保证内部 flex:1 区域可滑动
  max-height: 320px;
}

// 动画期间：离场页面脱离文档流，不参与 grid cell 高度计算
// 这样进场页决定容器高度，避免切换时高度被两页 max 擑高
.slide-forward-leave-active,
.slide-back-leave-active {
  position: absolute;
  inset: 0;
}

/* 主页布局：文件区可滚动，文件夹区固定底部 */
.main-page {
  /* 容器即 flex column */
}

.main-files-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
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

.main-folders-area {
  flex-shrink: 0;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  border-top: 1px solid #ecf0f2;
}

/* 详情页布局：头部固定不滚，内容区滚动 */
.detail-page {
  /* flex column */
}

.detail-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  user-select: none;
  border-bottom: 1px solid #ecf0f2;
}

.detail-back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover {
    background: rgba(15, 116, 255, 0.08);
    color: var(--color-text-primary);
  }
}

.detail-title {
  flex: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-title-icon {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.detail-count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-placeholder);
  background: rgba(0, 0, 0, 0.04);
  padding: 1px 8px;
  border-radius: 10px;
}

.detail-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
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

/* 共用：行项 */
.dropdown-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  color: var(--color-text-placeholder);
  font-size: 14px;
}

.dropdown-item {
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

.dropdown-item-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
}

.dropdown-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dropdown-item-name {
  font-size: 14px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
}

.dropdown-item-folder {
  font-size: 12px;
  color: var(--color-text-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: 50%;
}

/* 文件夹入口行 */
.dropdown-folder-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
  min-width: 0;
  user-select: none;

  &:hover,
  &.active {
    background-color: rgba(15, 116, 255, 0.08);
  }
}

.folder-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
}

.folder-label {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-placeholder);
  background: rgba(0, 0, 0, 0.04);
  padding: 1px 8px;
  border-radius: 10px;
}

/* 详情页行项：上下结构（名称 + 描述） */
.detail-item {
  align-items: flex-start;
}

.detail-item-content {
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 2px;
}

.detail-item-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-item-desc {
  font-size: 12px;
  color: var(--color-text-placeholder);
  line-height: 1.4;
  width: 100%;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

/* 双页面方向感知滑动 */
.slide-forward-enter-active,
.slide-forward-leave-active,
.slide-back-enter-active,
.slide-back-leave-active {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}

/* forward：主页 -> 详情页 */
.slide-forward-enter-from {
  transform: translateX(100%);
}
.slide-forward-leave-to {
  transform: translateX(-100%);
}

/* back：详情页 -> 主页 */
.slide-back-enter-from {
  transform: translateX(-100%);
}
.slide-back-leave-to {
  transform: translateX(100%);
}

/* 整体下拉显隐 */
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
