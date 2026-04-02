/**
 * AGUI 协议 SSE 相关类型定义
 * 参考 AGUI 原生对接 SSE 接口手册
 */

/** 基础事件 */
export interface BaseEvent {
  type: string
  timestamp?: number
  [key: string]: unknown
}

/** 运行开始 */
export interface RunStartedEvent extends BaseEvent {
  type: 'RUN_STARTED'
  threadId: string
  runId: string
  parentRunId?: string
  input?: unknown
}

/** 运行结束 */
export interface RunFinishedEvent extends BaseEvent {
  type: 'RUN_FINISHED'
  threadId: string
  runId: string
  result?: unknown
}

/** 运行错误 */
export interface RunErrorEvent extends BaseEvent {
  type: 'RUN_ERROR'
  message: string
  code?: string
}

/** 文本消息开始 */
export interface TextMessageStartEvent extends BaseEvent {
  type: 'TEXT_MESSAGE_START'
  messageId: string
  role: string
}

/** 文本消息内容增量 */
export interface TextMessageContentEvent extends BaseEvent {
  type: 'TEXT_MESSAGE_CONTENT'
  messageId: string
  delta: string
}

/** 文本消息结束 */
export interface TextMessageEndEvent extends BaseEvent {
  type: 'TEXT_MESSAGE_END'
  messageId: string
}

/** 文本消息分块（便捷事件，部分服务端直接发送） */
export interface TextMessageChunkEvent extends BaseEvent {
  type: 'TEXT_MESSAGE_CHUNK'
  messageId?: string
  role?: string
  delta?: string
}

/** 工具调用开始 */
export interface ToolCallStartEvent extends BaseEvent {
  type: 'TOOL_CALL_START'
  toolCallId: string
  toolCallName: string
  parentMessageId?: string
}

/** 工具调用参数增量 */
export interface ToolCallArgsEvent extends BaseEvent {
  type: 'TOOL_CALL_ARGS'
  toolCallId: string
  delta: string
}

/** 工具调用结束 */
export interface ToolCallEndEvent extends BaseEvent {
  type: 'TOOL_CALL_END'
  toolCallId: string
}

/** 工具调用结果 */
export interface ToolCallResultEvent extends BaseEvent {
  type: 'TOOL_CALL_RESULT'
  messageId: string
  toolCallId: string
  content: string
  role?: 'tool'
}

/** 状态快照 */
export interface StateSnapshotEvent extends BaseEvent {
  type: 'STATE_SNAPSHOT'
  snapshot: unknown
}

/** 状态增量（JSON Patch 操作数组） */
export interface StateDeltaEvent extends BaseEvent {
  type: 'STATE_DELTA'
  delta: Operation[]
}

/** JSON Patch 单条操作 */
export interface Operation {
  op: string
  path: string
  value?: unknown
}

/** 消息列表快照 */
export interface MessagesSnapshotEvent extends BaseEvent {
  type: 'MESSAGES_SNAPSHOT'
  messages: Message[]
}

/** 活动快照 */
export interface ActivitySnapshotEvent extends BaseEvent {
  type: 'ACTIVITY_SNAPSHOT'
  messageId: string
  activityType: string
  content: Record<string, unknown>
  replace?: boolean
}

/** 活动增量 */
export interface ActivityDeltaEvent extends BaseEvent {
  type: 'ACTIVITY_DELTA'
  messageId: string
  activityType: string
  patch: Operation[]
}

/** 自定义事件 */
export interface CustomEvent extends BaseEvent {
  type: 'CUSTOM'
  name: string
  value: unknown
}

/** 原始事件 */
export interface RawEvent extends BaseEvent {
  type: 'RAW'
  event: unknown
  source?: string
}

/** 消息角色 */
export type MessageRole = 'user' | 'assistant' | 'system' | 'tool' | 'activity' | 'error'

/** 消息 */
export interface Message {
  id: string
  role: MessageRole
  content?: string | unknown
  name?: string
  toolCalls?: ToolCall[]
  toolCallId?: string
  activityType?: string
}

/** 工具调用描述 */
export interface ToolCall {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

/** 工具定义（JSON Schema 参数） */
export interface Tool {
  name: string
  description: string
  parameters: unknown
}

/** 运行智能体请求体 */
export interface RunAgentInput {
  threadId: string
  runId: string
  parentRunId?: string
  messages: Message[]
  tools?: Tool[]
  context?: unknown[]
  state?: unknown
  forwardedProps?: Record<string, unknown>
}
