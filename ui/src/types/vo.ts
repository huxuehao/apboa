/**
 * VO类型定义
 *
 * @author huxuehao
 */

import type {
  Role,
  ToolChoiceStrategy,
  HookType,
  ToolType,
  AuthType,
  ModelType,
  HealthStatus,
  KbType,
  McpMode,
  McpProtocol,
  SensitiveWordAction,
  RAGMode
} from './enums'
import type {AgentA2A} from "@/types/entity.ts";

/**
 * 账号VO
 */
export interface AccountVO {
  id: string
  nickname: string
  email: string
  username: string
  enabled: boolean
  roles: Role[]
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

/**
 * 智能体定义VO
 */
export interface AgentDefinitionVO {
  id: string
  agentType: 'CUSTOM' | 'A2A'
  name: string
  agentCode: string
  description: string
  modelConfigId: string
  modelParamsOverride: Record<string, unknown> | null
  skill: string[]
  tool: string[]
  mcp: string[]
  hook: string[]
  subAgent: string[]
  knowledgeBase: string[]
  toolChoiceStrategy: ToolChoiceStrategy
  specificToolName: string
  systemPromptTemplateId: string
  followTemplate: boolean
  systemPrompt: string
  sensitiveWordConfigId: string
  sensitiveFilterEnabled: boolean
  maxIterations: number
  enablePlanning: boolean
  maxSubtasks: number
  requirePlanConfirmation: boolean
  enableMemory: boolean
  enableMemoryCompression: boolean
  memoryCompressionConfig: Record<string, unknown> | null
  structuredOutputEnabled: boolean
  structuredOutputSchema: Record<string, unknown> | null
  structuredOutputReminder: 'PROMPT' | 'TOOL_CHOICE'
  version: string
  tag: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
  agentA2A: AgentA2A
}

/**
 * Hook配置VO
 */
export interface HookConfigVO {
  id: string
  name: string
  hookType: HookType
  description: string
  classPath: string
  code: string
  priority: number
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 知识库配置VO
 */
export interface KnowledgeBaseConfigVO {
  id: string
  name: string
  kbType: KbType
  ragMode: RAGMode
  description: string
  connectionConfig: Record<string, unknown> | null
  endpointConfig: Record<string, unknown> | null
  retrievalConfig: Record<string, unknown> | null
  rerankingConfig: Record<string, unknown> | null
  queryRewriteConfig: Record<string, unknown> | null
  metadataFilters: Record<string, unknown> | null
  httpConfig: Record<string, unknown> | null
  healthStatus: HealthStatus
  lastSyncTime: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * MCP服务器VO
 */
export interface McpServerVO {
  id: string
  name: string
  protocol: McpProtocol
  mode: McpMode
  timeout: number
  protocolConfig: Record<string, unknown> | null
  description: string
  healthStatus: HealthStatus
  lastHealthCheck: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 模型配置VO
 */
export interface ModelConfigVO {
  id: string
  providerId: string
  name: string
  modelId: string
  modelType: ModelType[]
  description: string
  streaming: boolean
  thinking: boolean
  contextWindow: number
  maxTokens: number
  temperature: number
  topP: number
  topK: number
  repeatPenalty: number
  seed: string
  extendConfig: Record<string, any> | null
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 模型提供商VO
 */
export interface ModelProviderVO {
  id: string
  type: string
  name: string
  description: string
  baseUrl: string
  authType: AuthType
  apiKey: string
  envVarName: string
  enabled: boolean
  configMeta: Record<string, unknown> | null
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

/**
 * 敏感词配置VO
 */
export interface SensitiveWordConfigVO {
  id: string
  category: string
  name: string
  description: string
  words: string[] | null
  action: SensitiveWordAction
  replacement: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 技能包VO
 */
export interface SkillPackageVO {
  id: string
  name: string
  description: string
  skillContent: string
  category: string
  references: any[] | null
  examples: any[] | null
  scripts: any[] | null
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 系统提示词模板VO
 */
export interface SystemPromptTemplateVO {
  id: string
  category: string
  name: string
  description: string
  content: string
  usageCount: number
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 工具VO
 */
export interface ToolVO {
  id: string
  name: string
  toolId: string
  description: string
  category: string
  language: string
  toolType: ToolType
  inputSchema: any[] | null
  outputSchema: any[] | null
  classPath: string
  code: string
  needConfirm: boolean,
  version: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * 聊天会话VO
 */
export interface ChatSessionVO {
  id: string
  userId: string
  agentId: string
  currentMessageId: string | null
  title: string | null
  isPinned: boolean
  pinTime: string | null
  createdAt: string
  updatedAt: string
}

/**
 * 聊天消息VO
 */
export interface ChatMessageVO {
  id: string
  sessionId: string
  role: string
  content: string
  parentId: string | null
  path: string
  depth: number
  createdAt: string
}
