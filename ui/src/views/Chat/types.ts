import type { AgentDefinitionVO, ChatSessionVO, ChatMessageVO } from '@/types'

// 扩展消息类型用于展示（含流式标记）
export interface DisplayMessage {
  id: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  isStreaming?: boolean
}
