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
  McpActivationStatus,
  McpFailureSource,
  McpToolExposureMode,
  McpMode,
  McpProtocol,
  SensitiveWordAction,
  RAGMode
} from './enums'
import type {AgentA2A, HookConfig, JobInfo} from "@/types/entity.ts";

/**
 * 账号VO
 */
export interface AccountVO {
  id: string | number
  nickname: string
  email: string
  username: string
  enabled: boolean
  roles: Role[]
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 智能体定义VO
 */
export interface AgentDefinitionVO {
  id: string | number
  agentType: 'CUSTOM' | 'A2A'
  name: string
  agentCode: string
  description: string
  modelConfigId: string
  modelParamsOverride: Record<string, unknown> | null
  skill: string[]
  tool: string[]
  mcp: string[]
  mcpBindings: AgentMcpBindingVO[]
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
  showToolProcess: boolean
  memoryCompressionConfig: Record<string, unknown> | null
  enableLongTermMemory: boolean
  longTermMemoryConfig: Record<string, unknown> | null
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
  jobInfo: JobInfo
  studioConfigId: string | null
  codeExecutionConfigId: string | null
}

/**
 * Hook配置VO
 */
export interface HookConfigVO extends HookConfig{
  used: string[]
}

/**
 * 知识库配置VO
 */
export interface KnowledgeBaseConfigVO {
  id: string | number
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
  id: string | number
  name: string
  protocol: McpProtocol
  mode: McpMode
  timeout: number
  protocolConfig: Record<string, unknown> | null
  description: string
  healthStatus: HealthStatus
  lastHealthCheck: string
  activationStatus: McpActivationStatus
  activationMessage: string
  failureSource: McpFailureSource
  activationStatusChangedAt: string | null
  lastActivationTime: string | null
  lastToolSyncTime: string | null
  toolCount: number
  availableToolCount: number
  runtimeFailThreshold: number
  needsSync: boolean
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
}

/**
 * Agent MCP 绑定信息
 */
export interface AgentMcpBindingVO {
  mcpServerId: string
  exposureMode: McpToolExposureMode
  mcpToolIds: string[]
}

/**
 * MCP 工具 VO
 */
export interface McpToolVO {
  id: string | number
  mcpServerId: string | number
  toolName: string
  description: string
  inputSchema: Record<string, unknown> | null
  outputSchema: Record<string, unknown> | null
  enabled: boolean
  missing: boolean
  sort: number
  lastDiscoveredAt: string | null
  lastSeenAt: string | null
}

/**
 * 模型配置VO
 */
export interface ModelConfigVO {
  id: string | number
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
  connectivityStatus?: string
  connectivityMessage?: string
  lastConnectivityCheck?: string
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
  id: string | number
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
  id: string | number
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
  id: string | number
  name: string
  description: string
  category: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  used: string[]
  tools:string[]
}

/**
 * 技能包文件树节点
 */
export interface SkillFileTreeNode {
  /** 文件名或目录名 */
  name: string
  /** 相对路径，如 "scripts/helper.py" */
  path: string
  /** 是否目录 */
  directory: boolean
  /** DB id（入库文件才有，纯文件系统文件为 null） */
  fileId: string | null
  /** 文件类型（仅入库文件） */
  fileType: 'SKILL_MD' | 'REFERENCES' | 'EXAMPLES' | 'SCRIPTS' | null
  /** 文件扩展名，目录为空 */
  extension: string
  /** 文件大小（字节），目录为 0 */
  fileSize: number
  /** 子节点 */
  children: SkillFileTreeNode[]
  /** 文件内容（前端加载后填充） */
  content?: string
  /** 前端状态：是否有未保存修改 */
  dirty?: boolean
}

/**
 * 技能包导入结果
 */
export interface SkillImportResult {
  importedCount: number
  skippedCount: number
  totalCount: number
  hintMessage?: string | null
}

/**
 * 系统提示词模板VO
 */
export interface SystemPromptTemplateVO {
  id: string | number
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
  id: string | number
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
  id: string | number
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
  id: string | number
  sessionId: string
  role: string
  content: string
  parentId: string | null
  path: string
  depth: number
  createdAt: string
}

/**
 * Studio配置VO
 */
export interface StudioConfigVO {
  id: string
  url: string
  project: string
}

/**
 * 访问秘钥VO
 */
export interface SecretKeyVO {
  id: string | number
  name: string
  /** value 在列表中已脱敏，创建时返回完整值 */
  value: string
  expireTime: string | null
  remark: string
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

/**
 * 代码执行配置VO
 */
export interface CodeExecutionConfigVO {
  id: string
  configName: string
  workDir?: string
  uploadDir?: string
  autoUpload?: boolean
  enableShell?: boolean
  enableRead?: boolean
  enableWrite?: boolean
  command?: string[] | null
}

/**
 * 趋势数据项
 */
export interface TrendItem {
  date: string
  value: number
}

/**
 * 智能体统计分析VO
 */
export interface AgentStatisticsVO {
  sessionTrend: TrendItem[]
  activeUserTrend: TrendItem[]
  messageTrend: TrendItem[]
  avgRoundsTrend: TrendItem[]
}

/**
 * 工作空间文件树节点 VO
 */
export interface WorkspaceFileNode {
  /** 文件或文件夹名称 */
  name: string
  /** 文件在工作空间中的相对路径 */
  path: string
  /** 是否为目录 */
  directory: boolean
  /** 文件全名（仅文件有效，含后缀） */
  fullName?: string
  /** 文件后缀（仅文件有效，不含点号） */
  extension?: string
  /** 文件大小可读格式（仅文件有效，如 "1.5 MB"） */
  readableSize?: string
  /** 最后修改时间（格式：yyyy-MM-dd HH:mm:ss） */
  lastModified?: string
  /** 最后修改时间戳（毫秒） */
  lastModifiedTime?: string
  /** 子节点（仅目录有效） */
  children?: WorkspaceFileNode[]
}

/**
 *  模型检查结果
 */
export interface CheckModelResult {
  success: boolean
  message: string
}
