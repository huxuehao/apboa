/**
 * Vue 组合式 API：封装 AgentClient，提供响应式 messages/state/activity 与 run/abort 方法
 * 便于在对话页直接绑定并调用
 */

import { ref, onUnmounted } from 'vue'
import { createAgentClient } from '@/api/agui'
import type { Message, RunAgentInput } from '@/types'
import type { EventHandlers, ToolHandler } from '@/api/agui'

export interface UseAgentClientOptions {
  url?: string
  headers?: Record<string, string>
  /** 额外事件回调（在内部 sync 之后调用） */
  handlers?: EventHandlers
  toolHandlers?: Record<string, ToolHandler>
}

/**
 * 使用 AGUI AgentClient，状态与 client 同步为响应式
 * @param options 可选 URL、headers、handlers、toolHandlers
 * @returns 响应式状态与方法
 */
export function useAgentClient(options: UseAgentClientOptions = {}) {
  const messages = ref<Message[]>([])
  const state = ref<unknown>({})
  const activityMessages = ref<Record<string, Message>>({})
  const isRunning = ref(false)
  const error = ref<Error | null>(null)

  const syncFromClient = (client: ReturnType<typeof createAgentClient>) => {
    messages.value = [...client.messages]
    state.value = client.state
    activityMessages.value = { ...client.activityMessages }
  }

  const client = createAgentClient({
    ...options,
    handlers: {
      onEvent: (e) => {
        syncFromClient(client)
        options.handlers?.onEvent?.(e)
      },
      onRunStarted: (e) => {
        syncFromClient(client)
        options.handlers?.onRunStarted?.(e)
      },
      onRunFinished: (e) => {
        syncFromClient(client)
        options.handlers?.onRunFinished?.(e)
      },
      onRunError: (e) => {
        error.value = new Error(e.message)
        options.handlers?.onRunError?.(e)
      },
      onTextMessageStart: (e) => {
        options.handlers?.onTextMessageStart?.(e)
        syncFromClient(client)
      },
      onTextMessageContent: (e, text) => {
        options.handlers?.onTextMessageContent?.(e, text)
        syncFromClient(client)
      },
      onTextMessageEnd: (e, text) => {
        options.handlers?.onTextMessageEnd?.(e, text)
        syncFromClient(client)
      },
      onToolCallStart: options.handlers?.onToolCallStart,
      onToolCallArgs: options.handlers?.onToolCallArgs,
      onToolCallEnd: options.handlers?.onToolCallEnd,
      onToolCallResult: options.handlers?.onToolCallResult,
      onStateSnapshot: options.handlers?.onStateSnapshot,
      onStateDelta: options.handlers?.onStateDelta,
      onMessagesSnapshot: options.handlers?.onMessagesSnapshot,
      onActivitySnapshot: options.handlers?.onActivitySnapshot,
      onActivityDelta: options.handlers?.onActivityDelta,
      onCustom: options.handlers?.onCustom,
      onRaw: options.handlers?.onRaw
    },
    toolHandlers: options.toolHandlers
  })

  async function run(overrides?: Partial<RunAgentInput>) {
    isRunning.value = true
    error.value = null
    try {
      await client.run(overrides)
    } finally {
      isRunning.value = false
      syncFromClient(client)
    }
  }

  async function abort() {
    await client.abort()
    isRunning.value = false
  }

  function addUserMessage(content: string, id?: string) {
    const msg = client.addUserMessage(content, id)
    messages.value = [...client.messages]
    return msg
  }

  onUnmounted(() => {
    client.abort()
  })

  return {
    messages,
    state,
    activityMessages,
    isRunning,
    error,
    run,
    abort,
    addUserMessage,
    /** 需直接操作 client 时使用（如 set messages/state） */
    client
  }
}
