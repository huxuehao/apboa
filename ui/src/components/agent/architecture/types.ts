/**
 * 智能体架构图类型定义
 *
 * @author huxuehao
 */

import type {
  AgentDefinitionVO,
  ToolVO,
  HookConfigVO,
  SkillPackageVO,
  McpServerVO,
  KnowledgeBaseConfigVO,
  ModelConfigVO,
  ModelProviderVO,
  SystemPromptTemplateVO,
  SensitiveWordConfigVO
} from '@/types'

/**
 * 节点类型枚举
 */
export type NodeType =
  | 'center-agent'      // 中心智能体节点
  | 'category'          // 分类节点
  | 'tool-item'         // 工具项节点
  | 'hook-item'         // 钩子项节点
  | 'skill-item'        // 技能项节点
  | 'mcp-item'          // MCP项节点
  | 'knowledge-item'    // 知识库项节点
  | 'agent-item'        // 子智能体项节点
  | 'model'             // 模型节点
  | 'prompt'            // 提示词节点
  | 'advanced-config'   // 高级配置节点
  | 'sensitive-item'    // 敏感词节点

/**
 * 分类类型枚举
 */
export type CategoryType =
  | 'tool'
  | 'hook'
  | 'skill'
  | 'mcp'
  | 'knowledge'
  | 'sub-agent'
  | 'model'
  | 'prompt'
  | 'advanced'
  | 'sensitive'

/**
 * 分类配置
 */
export interface CategoryConfig {
  type: CategoryType
  label: string
  icon: string
  color: string
  bgColor: string
  borderColor: string
}

/**
 * 中心智能体节点数据
 */
export interface CenterAgentNodeData {
  agent: AgentDefinitionVO
}

/**
 * 分类节点数据
 */
export interface CategoryNodeData {
  category: CategoryType
  label: string
  count: number
  icon: string
  color: string
  bgColor: string
  borderColor: string
}

/**
 * 工具项节点数据
 */
export interface ToolItemNodeData {
  tool: ToolVO
}

/**
 * 钩子项节点数据
 */
export interface HookItemNodeData {
  hook: HookConfigVO
}

/**
 * 技能项节点数据
 */
export interface SkillItemNodeData {
  skill: SkillPackageVO
}

/**
 * MCP项节点数据
 */
export interface McpItemNodeData {
  mcp: McpServerVO
}

/**
 * 知识库项节点数据
 */
export interface KnowledgeItemNodeData {
  knowledge: KnowledgeBaseConfigVO
}

/**
 * 子智能体项节点数据
 */
export interface AgentItemNodeData {
  agent: AgentDefinitionVO
}

/**
 * 模型节点数据
 */
export interface ModelNodeData {
  modelConfig: ModelConfigVO | null
  provider: ModelProviderVO | null
  paramsOverride: Record<string, unknown> | null
}

/**
 * 提示词节点数据
 */
export interface PromptNodeData {
  promptTemplate: SystemPromptTemplateVO | null
  followTemplate: boolean
  systemPrompt: string
}

/**
 * 高级配置节点数据
 */
export interface AdvancedConfigNodeData {
  enablePlanning: boolean
  enableMemory: boolean
  enableMemoryCompression: boolean
  structuredOutputEnabled: boolean
  maxIterations: number
  maxSubtasks: number
}

/**
 * 敏感词节点数据
 */
export interface SensitiveItemNodeData {
  sensitive: SensitiveWordConfigVO
}

/**
 * 架构数据加载状态
 */
export interface ArchitectureLoadingState {
  agent: boolean
  tools: boolean
  hooks: boolean
  skills: boolean
  mcps: boolean
  knowledgeBases: boolean
  subAgents: boolean
  model: boolean
  prompt: boolean
  sensitive: boolean
}

/**
 * 架构数据
 */
export interface ArchitectureData {
  agent: AgentDefinitionVO | null
  tools: ToolVO[]
  hooks: HookConfigVO[]
  skills: SkillPackageVO[]
  mcps: McpServerVO[]
  knowledgeBases: KnowledgeBaseConfigVO[]
  subAgents: AgentDefinitionVO[]
  modelConfig: ModelConfigVO | null
  modelProvider: ModelProviderVO | null
  promptTemplate: SystemPromptTemplateVO | null
  sensitiveConfig: SensitiveWordConfigVO | null
}

/**
 * 布局配置
 */
export interface LayoutConfig {
  centerX: number
  centerY: number
  categoryRadius: number
  itemRadius: number
  itemSpacing: number
}

/**
 * 分类配置映射
 */
export const CATEGORY_CONFIGS: Record<CategoryType, Omit<CategoryConfig, 'type'>> = {
  tool: {
    label: '工具',
    icon: 'ToolOutlined',
    color: '#1890ff',
    bgColor: '#e6f7ff',
    borderColor: '#91d5ff'
  },
  hook: {
    label: '钩子',
    icon: 'LoginOutlined',
    color: '#eb2f96',
    bgColor: '#fff0f6',
    borderColor: '#ffadd2'
  },
  skill: {
    label: '技能包',
    icon: 'AppstoreOutlined',
    color: '#722ed1',
    bgColor: '#f9f0ff',
    borderColor: '#d3adf7'
  },
  mcp: {
    label: 'MCP服务',
    icon: 'CloudServerOutlined',
    color: '#52c41a',
    bgColor: '#f6ffed',
    borderColor: '#b7eb8f'
  },
  knowledge: {
    label: '知识库',
    icon: 'DatabaseOutlined',
    color: '#13c2c2',
    bgColor: '#e6fffb',
    borderColor: '#87e8de'
  },
  'sub-agent': {
    label: '子智能体',
    icon: 'RobotOutlined',
    color: '#5b69d6',
    bgColor: '#f0f5ff',
    borderColor: '#adc6ff'
  },
  model: {
    label: '模型配置',
    icon: 'ThunderboltOutlined',
    color: '#fa8c16',
    bgColor: '#fff7e6',
    borderColor: '#ffd591'
  },
  prompt: {
    label: '提示词',
    icon: 'FileTextOutlined',
    color: '#2f54eb',
    bgColor: '#f0f5ff',
    borderColor: '#adc6ff'
  },
  advanced: {
    label: '高级配置',
    icon: 'SettingOutlined',
    color: '#595959',
    bgColor: '#fafafa',
    borderColor: '#d9d9d9'
  },
  sensitive: {
    label: '敏感词',
    icon: 'SafetyCertificateOutlined',
    color: '#f5222d',
    bgColor: '#fff1f0',
    borderColor: '#ffa39e'
  }
}

/**
 * 节点尺寸配置
 */
export const NODE_SIZES = {
  center: { width: 280, height: 160 },
  category: { width: 140, height: 50 },
  item: { width: 240, height: 120 },
  model: { width: 280, height: 200 },
  prompt: { width: 280, height: 160 },
  advanced: { width: 280, height: 180 }
}
