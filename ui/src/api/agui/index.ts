/**
 * AGUI SSE 模块：工厂与统一导出
 */

import { getAgentRunURL, getSSEHeaders } from './request'
import { AgentClient } from './agent-client'
import type { EventHandlers, ToolHandler } from './agent-client'
import type { RunAgentInput } from '@/types'

export type { EventHandlers, ToolHandler, EventMiddleware } from './agent-client'
export { AgentClient } from './agent-client'

export interface CreateAgentClientOptions {
  /** 覆盖默认 run URL */
  url?: string
  /** 覆盖默认请求头（会与 SSE 基础头合并） */
  headers?: Record<string, string>
  /** 事件回调 */
  handlers?: EventHandlers
  /** 工具名 -> 执行函数 */
  toolHandlers?: Record<string, ToolHandler>
}

/**
 * 创建已配置 baseURL 与认证头的 AgentClient
 * @param options 可选 URL/headers/handlers/toolHandlers 覆盖
 * @returns AgentClient 实例
 */
export function createAgentClient(
  options: CreateAgentClientOptions = {}
): AgentClient {
  const url = options.url ?? getAgentRunURL()
  const headers = { ...getSSEHeaders(), ...options.headers }
  return new AgentClient(
    url,
    headers,
    options.handlers ?? {},
    options.toolHandlers ?? {}
  )
}

export type { RunAgentInput }
