// 扩展消息类型用于展示（含流式标记）
export interface DisplayMessage {
  id: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  createdAt?: string
  isStreaming?: boolean
  /** 推理内容 */
  reasoningContent?: string
  /** 推理消息 ID */
  reasoningMessageId?: string
  /** 推理是否还在流式进行中 */
  reasoningStreaming?: boolean
}
