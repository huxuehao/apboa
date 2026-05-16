/**
 * 资源类目注册表
 * 集中维护各 ResourceKind 的展示元数据与标签元数据
 * 新增资源类型仅需在此处补充注册项即可
 *
 * @author huxuehao
 */

import { computed, h, type Component, type Ref } from 'vue'
import {
  AppstoreOutlined,
  FolderOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'
import FileIcon from '@/components/workspace/FileIcon.vue'
import type { FlatFileItem } from '@/composables/chat/useWorkspaceFiles'
import type {
  AgentSkillItem,
  AgentToolItem,
  MentionResourceItem,
  ResourceKind
} from '@/types/chat-mention'

/**
 * 显示文本回填上下文
 * 用于从 tagContent 反推可读名称（如根据 id 找回工具/技能名）
 */
export interface DisplayResolveContext {
  workspaceFiles?: FlatFileItem[]
  agentTools?: AgentToolItem[]
  agentSkills?: AgentSkillItem[]
}

/**
 * 资源类目元数据
 */
export interface ResourceCategoryMeta {
  /** 资源类型 */
  kind: ResourceKind
  /** 嵌入到 contenteditable 的标签名（与后端约定） */
  tagName: string
  /** 类目展示名 */
  label: string
  /** 类目图标（"文件夹"形态） */
  folderIcon: Component
  /** 是否在主页以"文件夹"形式聚合到底部，false 则平铺到主区 */
  asFolder: boolean
  /** 主页"文件夹"区排序，越大越靠下；asFolder=false 时不参与排序 */
  order: number
  /** 单项行图标渲染 */
  renderItemIcon: (item: MentionResourceItem) => Component | ReturnType<typeof h>
  /** 提取嵌入到 modelValue 的标签内容 */
  resolveTagContent: (item: MentionResourceItem) => string
  /** 标签视觉显示文本（contenteditable 中可见文字） */
  resolveTagDisplay: (item: MentionResourceItem) => string
  /** 从 tagContent 反向解析显示文本（用于 modelValue 重新渲染时） */
  resolveDisplayFromContent: (content: string, ctx: DisplayResolveContext) => string
}

/**
 * 类目元数据注册表（按 kind 索引）
 */
export const RESOURCE_CATEGORY_REGISTRY: Record<ResourceKind, ResourceCategoryMeta> = {
  'workspace-file': {
    kind: 'workspace-file',
    tagName: 'workspace-file',
    label: '工作空间文件',
    folderIcon: FolderOutlined,
    asFolder: false,
    order: 0,
    renderItemIcon: (item) =>
      h(FileIcon, {
        fileName: (item.raw as FlatFileItem | undefined)?.fullName || item.name,
        width: 18
      }),
    resolveTagContent: (item) =>
      (item.raw as FlatFileItem | undefined)?.path || item.id,
    resolveTagDisplay: (item) => item.name,
    resolveDisplayFromContent: (content) => {
      const lastSlash = content.lastIndexOf('/')
      return lastSlash > -1 ? content.slice(lastSlash + 1) : content
    }
  },
  'agent-tool': {
    kind: 'agent-tool',
    tagName: 'agent-tool',
    label: '工具',
    folderIcon: ToolOutlined,
    asFolder: true,
    order: 10,
    renderItemIcon: () => h(ToolOutlined),
    // 工具直接以 name 作为 tagContent，便于消息侧无依赖渲染
    resolveTagContent: (item) => item.id,
    resolveTagDisplay: (item) => item.name,
    resolveDisplayFromContent: (content) => content
  },
  'agent-skill': {
    kind: 'agent-skill',
    tagName: 'agent-skill',
    label: '技能',
    folderIcon: AppstoreOutlined,
    asFolder: true,
    order: 20,
    renderItemIcon: () => h(AppstoreOutlined),
    // 技能直接以 name 作为 tagContent
    resolveTagContent: (item) => item.id,
    resolveTagDisplay: (item) => item.name,
    resolveDisplayFromContent: (content) => content
  }
}

/**
 * 按 tagName 反查 ResourceKind
 *
 * @param tagName 标签名
 * @return 命中的 kind，未命中返回 null
 */
export function findKindByTagName(tagName: string): ResourceKind | null {
  for (const k of Object.keys(RESOURCE_CATEGORY_REGISTRY) as ResourceKind[]) {
    if (RESOURCE_CATEGORY_REGISTRY[k].tagName === tagName) return k
  }
  return null
}

/**
 * 将原始数据归一化为统一资源项
 *
 * @param kind 资源类型
 * @param raw 原始数据
 * @return 归一化资源项
 */
export function toResourceItem(
  kind: ResourceKind,
  raw: FlatFileItem | AgentToolItem | AgentSkillItem
): MentionResourceItem {
  if (kind === 'workspace-file') {
    const f = raw as FlatFileItem
    return {
      kind,
      id: f.path,
      name: f.fullName || f.name,
      description: f.folderPath || undefined,
      raw: f
    }
  }
  const r = raw as AgentToolItem | AgentSkillItem
  return {
    kind,
    id: r.id,
    name: r.name,
    description: r.description,
    raw: r
  }
}

/**
 * 资源源数据（响应式 ref）
 */
export interface ResourceSources {
  workspaceFiles: Ref<FlatFileItem[]>
  agentTools: Ref<AgentToolItem[]>
  agentSkills: Ref<AgentSkillItem[]>
}

/**
 * 主页"文件夹"区段
 */
export interface MainCategorySection {
  /** 资源类型 */
  kind: ResourceKind
  /** 元数据 */
  meta: ResourceCategoryMeta
  /** 该类目下的资源项 */
  items: MentionResourceItem[]
}

/**
 * 资源类目数据 composable
 * 输入响应式数据源，输出主页平铺项与文件夹区段
 *
 * @param sources 响应式数据源
 * @return flatItems / folderSections
 */
export function useResourceCategories(sources: ResourceSources) {
  /**
   * 平铺类目（主页主区域，asFolder=false 的所有资源）
   */
  const flatItems = computed<MentionResourceItem[]>(() => {
    const out: MentionResourceItem[] = []
    for (const k of Object.keys(RESOURCE_CATEGORY_REGISTRY) as ResourceKind[]) {
      const meta = RESOURCE_CATEGORY_REGISTRY[k]
      if (meta.asFolder) continue
      const list = pickRawList(k, sources)
      for (const raw of list) {
        out.push(toResourceItem(k, raw))
      }
    }
    return out
  })

  /**
   * 文件夹类目（主页底部，asFolder=true）
   */
  const folderSections = computed<MainCategorySection[]>(() => {
    const sections: MainCategorySection[] = []
    for (const k of Object.keys(RESOURCE_CATEGORY_REGISTRY) as ResourceKind[]) {
      const meta = RESOURCE_CATEGORY_REGISTRY[k]
      if (!meta.asFolder) continue
      const list = pickRawList(k, sources)
      sections.push({
        kind: k,
        meta,
        items: list.map((raw) => toResourceItem(k, raw))
      })
    }
    sections.sort((a, b) => a.meta.order - b.meta.order)
    return sections
  })

  return { flatItems, folderSections }
}

/**
 * 取出指定 kind 的原始数据列表
 */
function pickRawList(
  kind: ResourceKind,
  sources: ResourceSources
): Array<FlatFileItem | AgentToolItem | AgentSkillItem> {
  if (kind === 'workspace-file') return sources.workspaceFiles.value
  if (kind === 'agent-tool') return sources.agentTools.value
  if (kind === 'agent-skill') return sources.agentSkills.value
  return []
}
