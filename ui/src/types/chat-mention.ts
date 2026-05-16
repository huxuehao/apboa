/**
 * 聊天 @mention 资源类型定义
 * 支持工作空间文件、Agent 工具、Agent 技能等多种可被引用的资源
 *
 * @author huxuehao
 */

/**
 * 资源类型枚举
 * 新增 kind 时只需：
 * 1. 在此处增加字面量
 * 2. 在 useResourceCategories 注册表中补充元数据
 * 3. 如需在消息侧渲染，在 TaggedContentRenderer 中注册组件
 */
export type ResourceKind = 'workspace-file' | 'agent-tool' | 'agent-skill'

/**
 * Agent 工具项（用于 mock 与后续接入）
 */
export interface AgentToolItem {
  /** 工具唯一标识 */
  id: string
  /** 工具名称 */
  name: string
  /** 工具描述 */
  description?: string
}

/**
 * Agent 技能项
 */
export interface AgentSkillItem {
  /** 技能唯一标识 */
  id: string
  /** 技能名称 */
  name: string
  /** 技能描述 */
  description?: string
}

/**
 * 流转于 Dropdown -> Editor 的统一资源项
 * 任意 ResourceKind 的资源都被归一化为此结构
 */
export interface MentionResourceItem {
  /** 资源类型 */
  kind: ResourceKind
  /** 同 kind 内唯一标识 */
  id: string
  /** 主标题 */
  name: string
  /** 副标题（描述、路径等） */
  description?: string
  /** 原始数据，便于业务侧二次处理 */
  raw?: unknown
}
