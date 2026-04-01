/**
 * Entity类型定义
 *
 * @author huxuehao
 */

import type { BaseEntity } from './common'
import type {
  Role,
  ToolChoiceStrategy,
  HookType,
  ToolType,
  CodeLanguage,
  AuthType,
  ModelProviderType,
  ModelType,
  HealthStatus,
  KbType,
  McpMode,
  McpProtocol,
  SensitiveWordAction,
  RAGMode,
  A2aType
} from './enums'

/**
 * 账号
 */
export interface Account extends BaseEntity {
  nickname: string
  email: string
  username: string
  password: string
}

/**
 * 账号角色
 */
export interface AccountRole {
  id: string
  accountId: string
  role: Role
}

/**
 * 智能体定义
 */
export interface AgentDefinition extends BaseEntity {
  agentType: 'CUSTOM' | 'A2A'
  name: string
  agentCode: string
  description: string
  modelConfigId: string
  modelParamsOverride: Record<string, any> | null
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
  showToolProcess: boolean
  enableMemoryCompression: boolean
  memoryCompressionConfig: Record<string, any> | null
  structuredOutputEnabled: boolean
  structuredOutputReminder: 'PROMPT' | 'TOOL_CHOICE'
  structuredOutputSchema: string
  version: string
  tag: string | null
}

/**
 * 智能体与Hook关联
 */
export interface AgentHook {
  id: string
  agentDefinitionId: string
  hookConfigId: string
}

/**
 * 智能体与知识库关联
 */
export interface AgentKnowledgeBase {
  id: string
  agentDefinitionId: string
  knowledgeBaseConfigId: string
}

/**
 * 智能体与MCP服务器关联
 */
export interface AgentMcpServer {
  id: string
  agentDefinitionId: string
  mcpServerId: string
}

/**
 * 智能体与技能包关联
 */
export interface AgentSkillPackage {
  id: string
  agentDefinitionId: string
  skillPackageId: string
}

/**
 * 智能体与子智能体关联
 */
export interface AgentSubAgent {
  id: string
  parentAgentId: string
  subAgentId: string
}

/**
 * 智能体与工具关联
 */
export interface AgentTool {
  id: string
  agentDefinitionId: string
  toolId: string
}

/**
 * Hook配置
 */
export interface HookConfig extends BaseEntity {
  name: string
  hookType: HookType
  description: string
  classPath: string
  code: string
  priority: number
}

/**
 * 知识库配置
 */
export interface KnowledgeBaseConfig extends BaseEntity {
  name: string
  kbType: KbType
  ragMode: RAGMode
  description: string
  connectionConfig: Record<string, any> | null
  endpointConfig: Record<string, any> | null
  retrievalConfig: Record<string, any> | null
  rerankingConfig: Record<string, any> | null
  queryRewriteConfig: Record<string, any> | null
  metadataFilters: Record<string, any> | null
  httpConfig: Record<string, any> | null
  healthStatus: HealthStatus
  lastSyncTime: string
}

/**
 * MCP服务器配置
 */
export interface McpServer extends BaseEntity {
  name: string
  protocol: McpProtocol
  mode: McpMode
  timeout: number
  protocolConfig: Record<string, any> | null
  description: string
  healthStatus: HealthStatus
  lastHealthCheck: string
}

/**
 * 模型配置
 */
export interface ModelConfig extends BaseEntity {
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
}

/**
 * 模型提供商
 */
export interface ModelProvider extends BaseEntity {
  type: ModelProviderType
  name: string
  description: string
  baseUrl: string
  authType: AuthType
  apiKey: string
  envVarName: string
  configMeta: Record<string, any> | null
}

/**
 * 敏感词配置
 */
export interface SensitiveWordConfig extends BaseEntity {
  category: string
  name: string
  description: string
  words: string[] | null
  action: SensitiveWordAction
  replacement: string
}

/**
 * 技能包
 */
export interface SkillPackage extends BaseEntity {
  name: string
  description: string
  skillContent: string
  category: string
  references: any[]| null
  examples: any[] | null
  scripts: any[] | null
}

/**
 * 系统提示词模板
 */
export interface SystemPromptTemplate extends BaseEntity {
  category: string
  name: string
  description: string
  content: string
  usageCount: number
}

/**
 * 工具配置
 */
export interface ToolConfig extends BaseEntity {
  name: string
  toolId: string
  description: string
  category: string
  toolType: ToolType
  needConfirm: boolean
  inputSchema: any[] | null
  outputSchema: any[] | null
  classPath: string | null
  language: CodeLanguage | null
  code: string
  version: string
}

/**
 * 聊天会话（current_message_id 表示当前查看/继续对话的叶子节点）
 */
export interface ChatSession {
  id: string
  userId: string
  agentId: string
  currentMessageId: string | null
  title: string | null
  isPinned: boolean
  pinTime: string | null
  createdAt: string
  updatedAt: string
  deleted: boolean
}

/**
 * 聊天消息（树结构，path 为物化路径）
 */
export interface ChatMessage {
  id: string
  sessionId: string
  role: string
  content: string
  parentId: string | null
  path: string
  depth: number
  createdAt: string
}

/**
 * 已上传附件项（用于聊天输入框展示）
 */
export interface UploadedFileItem {
  id: string
  name: string
  extension: string
  size: string
  /** 是否正在上传中（上传完成后为 false 或 undefined） */
  uploading?: boolean
}

/**
 * 附件
 */
export interface Attach {
  id: string
  fileId: string
  link: string
  domain: string
  name: string
  originalName: string
  extension: string
  attachSize: number
  path: string
  createBy: string
  createAt: string
  updateBy: string
  updateAt: string
  protocol: string
  status: number
}

/**
 * 附件操作日志
 */
export interface AttachLog {
  id: string
  fileId: string
  originalName: string
  extension: string
  attachSize: number
  optUser: string
  optUserName: string
  optTime: string
  optIp: string
  optType: string
}

/**
 * 文件存储协议配置
 */
export interface StorageProtocol {
  id: string
  name: string
  protocol: string
  protocolConfig: string
  createBy: string
  createAt: string
  updateBy: string
  updateAt: string
  remark: string
  valid: number
}

/**
 * 系统参数
 */
export interface Params {
  id: string
  paramName: string
  paramKey: string
  paramValue: string
}

/**
 * KvMap 键值对，支持环境变量模式
 */
export interface KvMap {
  key: string
  value: string
  evn: boolean
}

/**
 * A2A WellKnown 协议配置
 */
export interface WellKnownAgentConfig {
  agentName: string
  baseUrl: string
  relativeCardPath: string
  authHeaders: KvMap[]
}

/**
 * A2A Nacos 协议配置
 */
export interface NacosAgentConfig {
  agentName: string
  nacosProperties: KvMap[]
}

/**
 * AgentA2A 实体 — 智能体与 A2A 协议的关联配置
 */
export interface AgentA2A {
  id?: string
  agentDefinitionId?: string
  a2aType: A2aType
  a2aConfig: WellKnownAgentConfig | NacosAgentConfig
}

/**
 * 定时任务 实体
 */
export interface JobInfo {
  id?: string
  type: string
  bizId: string
  cron: string
  jobClass: string // com.hxh.apboa.job.scheduler.AgentScheduler
  dataMap: string // {"agentId":"xxxx", "input":"xxxx"}
  enabled: boolean // 0表示未启动，1表示启动
}

/**
 * StudioConfig 实体
 */
export interface StudioConfig {
  id?: string
  url: string
  project: string
}

/**
 * CodeExecutionConfig 实体 - 代码执行环境配置
 */
export interface CodeExecutionConfig {
  id?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
  configName: string // 配置名称，便于识别
  workDir?: string // 工作目录，空则使用临时目录
  uploadDir?: string // 脚本上传目录，空则使用work_dir/skills
  autoUpload?: boolean // 是否自动上传skill文件
  enableShell?: boolean // 是否启用ShellCommandTool
  enableRead?: boolean // 是否启用ReadFileTool
  enableWrite?: boolean // 是否启用WriteFileTool
  command?: string[] | null // 允许执行的命令，如 python3、bash
}
