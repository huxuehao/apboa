/**
 * DTO类型定义
 *
 * @author huxuehao
 */

import type { PageParams } from './common'
import type { HookType, ToolType, ModelType, KbType, McpProtocol } from './enums'

/**
 * 账号查询DTO
 */
export interface AccountDTO extends PageParams {
  nickname?: string
  email?: string
  username?: string
  enabled?: boolean
}

/**
 * 智能体定义查询DTO
 */
export interface AgentDefinitionDTO extends PageParams {
  name?: string
  agentType?: 'CUSTOM' | 'A2A'
  agentCode?: string
  enabled?: boolean
  tag?: string
}

/**
 * Hook配置查询DTO
 */
export interface HookConfigDTO extends PageParams {
  name?: string
  hookType?: HookType
  enabled?: boolean
}

/**
 * 知识库配置查询DTO
 */
export interface KnowledgeBaseConfigDTO extends PageParams {
  name?: string
  kbType?: KbType
  enabled?: boolean
}

/**
 * MCP服务器查询DTO
 */
export interface McpServerDTO extends PageParams {
  name?: string
  protocol?: McpProtocol
  enabled?: boolean
}

/**
 * 模型配置查询DTO
 */
export interface ModelConfigDTO extends PageParams {
  providerId?: string
  name?: string
  enabled?: boolean
}

/**
 * 模型提供商查询DTO
 */
export interface ModelProviderDTO extends PageParams {
  name?: string
  type?: string
  enabled?: boolean
}

/**
 * 敏感词配置查询DTO
 */
export interface SensitiveWordConfigDTO extends PageParams {
  category?: string
  name?: string
  enabled?: boolean
}

/**
 * 技能包查询DTO
 */
export interface SkillPackageDTO extends PageParams {
  name?: string
  category?: string
  enabled?: boolean
}

/**
 * 本地导入技能包配置
 */
export interface LocalImportConfig {
  /** 技能分类 */
  category: string
  /** 本地路径 */
  path: string
  /** 是否覆盖已存在的同名技能 */
  cover: boolean
}

/**
 * Git导入技能包配置
 */
export interface GitImportConfig {
  /** 技能分类 */
  category: string
  /** 仓库地址 */
  repoUrl: string
  /** 是否覆盖已存在的同名技能 */
  cover: boolean
}

/**
 * 系统提示词模板查询DTO
 */
export interface SystemPromptTemplateDTO extends PageParams {
  category?: string
  name?: string
  enabled?: boolean
}

/**
 * 工具查询DTO
 */
export interface ToolDTO extends PageParams {
  name?: string
  toolId?: string
  toolType?: ToolType
  category?: string
  enabled?: boolean
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 用户详情（登录返回）
 */
export interface UserDetail {
  id: string
  name: string
  account: string
  email: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  accessToken: string
  accessTokenTTL: string
  refreshToken: string
  refreshTokenTTL: string
  userDetail: UserDetail
}

/**
 * 刷新Token请求
 */
export interface RefreshTokenRequest {
  refreshToken: string
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  nickname: string
  username: string
  email: string
  password: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
}

/**
 * 创建会话DTO
 */
export interface ChatSessionCreateDTO {
  agentId: string
  title?: string
}

/**
 * 追加消息DTO（正常对话或重新生成）
 */
export interface ChatMessageAppendDTO {
  role: string
  content: string
}

/**
 * 会话列表查询DTO
 */
export interface ChatSessionQueryDTO {
  userId?: string
  agentId?: string
  isPinned?: boolean
  current?: number
  size?: number
}
