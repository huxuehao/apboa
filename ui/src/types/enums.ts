/**
 * 枚举类型定义
 *
 * @author huxuehao
 */

/**
 * 角色
 */
export enum Role {
  READ_ONLY = 'READ_ONLY',
  EDIT = 'EDIT',
  ADMIN = 'ADMIN'
}

/**
 * 工具选择策略
 */
export enum ToolChoiceStrategy {
  AUTO = 'AUTO',
  NONE = 'NONE',
  REQUIRED = 'REQUIRED',
  SPECIFIC = 'SPECIFIC'
}

/**
 * Hook类型
 */
export enum HookType {
  BUILTIN = 'BUILTIN',
  CUSTOM = 'CUSTOM'
}

/**
 * 工具类型
 */
export enum ToolType {
  BUILTIN = 'BUILTIN',
  CUSTOM = 'CUSTOM'
}

/**
 * 代码语言
 */
export enum CodeLanguage {
  JAVA = 'JAVA',
  JAVASCRIPT = 'JAVASCRIPT'
}

/**
 * 认证类型
 */
export enum AuthType {
  CONFIG = 'CONFIG',
  ENV = 'ENV'
}

/**
 * 模型提供商类型
 */
export enum ModelProviderType {
  DASH_SCOPE = 'DASH_SCOPE',
  OPEN_AI = 'OPEN_AI',
  ANTHROPIC = 'ANTHROPIC',
  GEMINI = 'GEMINI',
  OLLAMA = 'OLLAMA'
}

/**
 * 模型类型
 */
export enum ModelType {
  CHAT = 'CHAT',
  IMAGE = 'IMAGE',
  VIDEO = 'VIDEO',
  TTS = 'TTS',
  EMBEDDING = 'EMBEDDING',
  RERANKER = 'RERANKER'
}

/**
 * 健康状态
 */
export enum HealthStatus {
  HEALTHY = 'HEALTHY',
  UNHEALTHY = 'UNHEALTHY',
  UNKNOWN = 'UNKNOWN'
}

/**
 * 知识库类型
 */
export enum KbType {
  BAILIAN = 'BAILIAN',
  DIFY = 'DIFY',
  RAGFLOW = 'RAGFLOW'
}

/**
 * MCP运行模式
 */
export enum McpMode {
  SYNC = 'SYNC',
  ASYNC = 'ASYNC'
}

/**
 * MCP协议
 */
export enum McpProtocol {
  HTTP = 'HTTP',
  SSE = 'SSE',
  STDIO = 'STDIO'
}

/**
 * 敏感词处理动作
 */
export enum SensitiveWordAction {
  BLOCK = 'BLOCK',
  REPLACE = 'REPLACE',
  WARN = 'WARN'
}

/**
 * RAG模式
 */
export enum RAGMode {
  GENERIC = 'GENERIC',
  AGENTIC = 'AGENTIC'
}
