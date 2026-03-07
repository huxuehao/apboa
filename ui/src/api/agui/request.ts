/**
 * AGUI SSE 请求：基础 URL 与认证头
 * 独立于 axios 实例，供 fetch + ReadableStream 使用
 */

import { getToken } from '@/utils/auth'
import setting from '@/config/setting'

/** 默认 run 端点路径 */
const DEFAULT_RUN_PATH = '/api/apboa/agui/run'

/**
 * 获取智能体 run 的完整 URL
 * 开发环境直接请求后端，避免 Vite 代理缓冲 SSE 流
 */
export function getAgentRunURL(): string {
  const base = (import.meta.env.VITE_APP_BASE_API as string) || ''
  const path = base.endsWith('/') ? DEFAULT_RUN_PATH.slice(1) : DEFAULT_RUN_PATH
  if (base) return `${base}${path}`
  return path
}

/**
 * 获取 SSE 请求所需头（Content-Type、Accept、Authorization）
 * @returns 请求头对象
 */
export function getSSEHeaders(): Record<string, string> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream'
  }
  if (token) {
    headers[setting.tokenHeader] = `Bearer ${token}`
  }
  return headers
}
