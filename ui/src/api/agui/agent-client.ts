/**
 * AGUI 协议 SSE AgentClient：建连、解析流、事件分发、状态维护、工具调用、中止与中间件
 */

import { applyPatch } from 'fast-json-patch'
import type {
  BaseEvent,
  RunStartedEvent,
  RunFinishedEvent,
  RunErrorEvent,
  TextMessageStartEvent,
  TextMessageContentEvent,
  TextMessageEndEvent,
  TextMessageChunkEvent,
  ToolCallStartEvent,
  ToolCallArgsEvent,
  ToolCallEndEvent,
  ToolCallResultEvent,
  StateSnapshotEvent,
  StateDeltaEvent,
  MessagesSnapshotEvent,
  ActivitySnapshotEvent,
  ActivityDeltaEvent,
  CustomEvent,
  RawEvent,
  Message,
  RunAgentInput
} from '@/types'
import { refreshToken as refreshTokenApi } from "@/api/auth";
import {
  getToken,
  getRefreshToken,
  setToken,
  setRefreshToken,
  removeToken,
  removeRefreshToken
} from "@/utils/auth";


/** 事件处理器集合 */
export interface EventHandlers {
  onRunStarted?: (event: RunStartedEvent) => void
  onRunFinished?: (event: RunFinishedEvent) => void
  onRunError?: (event: RunErrorEvent) => void
  onTextMessageStart?: (event: TextMessageStartEvent) => void
  onTextMessageContent?: (event: TextMessageContentEvent, currentText: string) => void
  onTextMessageEnd?: (event: TextMessageEndEvent, finalText: string) => void
  onToolCallStart?: (event: ToolCallStartEvent) => void
  onToolCallArgs?: (event: ToolCallArgsEvent, partialArgs: string) => void
  onToolCallEnd?: (event: ToolCallEndEvent, fullArgs: unknown) => void
  onToolCallResult?: (event: ToolCallResultEvent) => void
  onStateSnapshot?: (event: StateSnapshotEvent) => void
  onStateDelta?: (event: StateDeltaEvent) => void
  onMessagesSnapshot?: (event: MessagesSnapshotEvent) => void
  onActivitySnapshot?: (event: ActivitySnapshotEvent) => void
  onActivityDelta?: (event: ActivityDeltaEvent) => void
  onCustom?: (event: CustomEvent) => void
  onRaw?: (event: RawEvent) => void
  onEvent?: (event: BaseEvent) => void
}

/** 工具执行函数 */
export type ToolHandler = (args: unknown) => Promise<unknown>

/** 事件中间件：接收事件与 next，可先处理再调用 next(event) */
export type EventMiddleware = (event: BaseEvent, next: (e: BaseEvent) => void) => void

interface ToolCallBuffer {
  name: string
  argsBuffer: string
  parentMessageId?: string
}

export class AgentClient {
  public messages: Message[] = []
  public state: unknown = {}
  public activityMessages: Record<string, Message> = {}

  private abortController: AbortController | null = null
  private messageBuffers: Record<string, string> = {}
  private toolCallBuffers: Record<string, ToolCallBuffer> = {}
  private middlewares: EventMiddleware[] = []
  /** TEXT_MESSAGE_CHUNK 展开时追踪当前消息 ID */
  private chunkCurrentMessageId: string | null = null

  constructor(
    private url: string,
    private headers: Record<string, string> = {},
    private handlers: EventHandlers = {},
    private toolHandlers: Record<string, ToolHandler> = {}
  ) {}

  /**
   * 注册事件中间件
   * @param mw 中间件函数
   */
  use(mw: EventMiddleware): void {
    this.middlewares.push(mw)
  }

  /**
   * 构建 RunAgentInput，支持 overrides 覆盖
   */
  private buildInput(overrides?: Partial<RunAgentInput>): RunAgentInput {
    return {
      threadId: overrides?.threadId ?? `thread_${Date.now()}`,
      runId:
        overrides?.runId ??
        `run_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
      messages: this.messages,
      state: this.state,
      tools: overrides?.tools ?? [],
      context: overrides?.context ?? [],
      forwardedProps: overrides?.forwardedProps ?? {},
      ...overrides
    }
  }

  /**
   * 发起一次 run 请求并消费 SSE 流
   */
  async run(overrides?: Partial<RunAgentInput>): Promise<void> {
    this.abortController = new AbortController()

    // 最大重试次数
    const MAX_RETRIES = 1
    let retryCount = 0

    const executeRequest = async (): Promise<void> => {
      try {
        const input: RunAgentInput = this.buildInput(overrides)
        const response = await fetch(this.url + "/" + input.forwardedProps?.agentCode, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
            ...this.headers
          },
          body: JSON.stringify(input),
          signal: this.abortController?.signal
        })

        // 处理 401 未授权
        if (response.status === 401) {
          if (retryCount < MAX_RETRIES) {
            retryCount++

            // 尝试刷新 token
            try {
              const refreshToken = getRefreshToken()
              if (!refreshToken) {
                window.location.href = "/#/login";
                window.location.reload();
                return
              }

              // 调用刷新 token 的接口
              const refreshResponse = await fetch('/api/auth/refresh-token', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json',
                },
                body: JSON.stringify({ refreshToken })
              })

              if (!refreshResponse.ok) {
                window.location.href = "/#/login";
              }

              const data = await refreshResponse.json()
              const newToken = data.data?.accessToken || data.accessToken
              const newRefreshToken = data.data?.refreshToken || data.refreshToken

              if (newToken && newRefreshToken) {
                setToken(newToken);
                setRefreshToken(newRefreshToken);
                // 重试原始请求
                await executeRequest()
                return
              } else {
                window.location.href = "/#/login";
                window.location.reload();
              }
            } catch (refreshError) {
              console.error('Token refresh failed:', refreshError)
              removeToken()
              removeRefreshToken()
              window.location.href = "/#/login";
              window.location.reload();
              return
            }
          } else {
            window.location.href = "/#/login";
            window.location.reload();
            return
          }
        }

        if (!response.ok) {
          const errorEvent: RunErrorEvent = {
            type: 'RUN_ERROR',
            message: `HTTP ${response.status}`,
            code: String(response.status)
          }
          this.handleEvent(errorEvent)
          return
        }

        if (!response.body) {
          throw new Error('Response body is null')
        }

        await this.readStream(response.body.getReader())
      } catch (err: unknown) {
        if (err instanceof Error && err.name === 'AbortError') {
          // 用户中止，不当作 RUN_ERROR
          return
        }
        throw err // 向上抛出错误，由外层 catch 处理
      }
    }

    try {
      await executeRequest()
    } catch (err: unknown) {
      const errorEvent: RunErrorEvent = {
        type: 'RUN_ERROR',
        message: err instanceof Error ? err.message : String(err)
      }
      this.handleEvent(errorEvent)
    } finally {
      this.abortController = null
    }
  }

  /**
   * 读取 SSE 流并按行解析 data: 行，派发事件
   */
  private async readStream(
    reader: ReadableStreamDefaultReader<Uint8Array>
  ): Promise<void> {
    const decoder = new TextDecoder()
    let buffer = ''

    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.replace(/^data:\s*/, '').trim()
            if (!data) continue
            try {
              let parsed: unknown = JSON.parse(data)
              if (typeof parsed === 'string') {
                parsed = JSON.parse(parsed.trim())
              }
              const event = parsed as BaseEvent
              if (event?.type) {
                this.handleEvent(event)
              }
            } catch (e) {
              console.warn('SSE parse:', data.slice(0, 80), e)
              this.handlers.onRaw?.({ type: 'RAW', event: data, source: 'sse' })
            }
          }
        }
      }
    } finally {
      reader.releaseLock()
    }
  }

  /**
   * 经中间件链后执行 processEvent
   */
  private handleEvent(event: BaseEvent): void {
    this.handlers.onEvent?.(event)

    const dispatch = (index: number) => (e: BaseEvent) => {
      if (index < this.middlewares.length) {
        this.middlewares[index]?.(e, dispatch(index + 1))
      } else {
        this.processEvent(e)
      }
    }
    dispatch(0)(event)
  }

  /**
   * 按 event.type 更新内部状态并调用对应 handler
   */
  private processEvent(event: BaseEvent): void {
    switch (event.type) {
      case 'RUN_STARTED':
        this.chunkCurrentMessageId = null
        this.handlers.onRunStarted?.(event as RunStartedEvent)
        break
      case 'RUN_FINISHED':
      case 'RUN_ERROR':
        this.chunkCurrentMessageId = null
        if (event.type === 'RUN_FINISHED') {
          this.handlers.onRunFinished?.(event as RunFinishedEvent)
        } else {
          this.handlers.onRunError?.(event as RunErrorEvent)
        }
        break

      case 'TEXT_MESSAGE_START': {
        const e = event as TextMessageStartEvent
        this.messageBuffers[e.messageId] = ''
        this.messages.push({
          id: e.messageId,
          role: e.role as Message['role'],
          content: ''
        })
        this.handlers.onTextMessageStart?.(e)
        break
      }
      case 'TEXT_MESSAGE_CONTENT': {
        const e = event as TextMessageContentEvent
        if (this.messageBuffers[e.messageId] === undefined) {
          this.messageBuffers[e.messageId] = ''
          this.messages.push({
            id: e.messageId,
            role: 'assistant' as Message['role'],
            content: ''
          })
          this.handlers.onTextMessageStart?.({
            type: 'TEXT_MESSAGE_START',
            messageId: e.messageId,
            role: 'assistant'
          })
        }
        this.messageBuffers[e.messageId] += e.delta
        const msg = this.messages.find((m) => m.id === e.messageId)
        if (msg) msg.content = this.messageBuffers[e.messageId]
        this.handlers.onTextMessageContent?.(e, this.messageBuffers[e.messageId])
        break
      }
      case 'TEXT_MESSAGE_END': {
        const e = event as TextMessageEndEvent
        const full = this.messageBuffers[e.messageId] ?? ''
        delete this.messageBuffers[e.messageId]
        this.chunkCurrentMessageId = null
        const msgEnd = this.messages.find((m) => m.id === e.messageId)
        if (msgEnd) msgEnd.content = full
        this.handlers.onTextMessageEnd?.(e, full)
        break
      }

      case 'TEXT_MESSAGE_CHUNK': {
        const e = event as TextMessageChunkEvent
        const msgId = e.messageId ?? this.chunkCurrentMessageId
        const delta = e.delta ?? ''

        if (e.messageId && e.messageId !== this.chunkCurrentMessageId) {
          if (this.chunkCurrentMessageId) {
            const prevFull = this.messageBuffers[this.chunkCurrentMessageId] ?? ''
            delete this.messageBuffers[this.chunkCurrentMessageId]
            const prevMsg = this.messages.find((m) => m.id === this.chunkCurrentMessageId)
            if (prevMsg) prevMsg.content = prevFull
            this.handlers.onTextMessageEnd?.(
              { type: 'TEXT_MESSAGE_END', messageId: this.chunkCurrentMessageId },
              prevFull
            )
          }
          this.chunkCurrentMessageId = e.messageId
          this.messageBuffers[e.messageId] = ''
          this.messages.push({
            id: e.messageId,
            role: (e.role ?? 'assistant') as Message['role'],
            content: ''
          })
          this.handlers.onTextMessageStart?.({
            type: 'TEXT_MESSAGE_START',
            messageId: e.messageId,
            role: e.role ?? 'assistant'
          })
        }

        if (msgId && delta) {
          this.messageBuffers[msgId] = (this.messageBuffers[msgId] ?? '') + delta
          const msg = this.messages.find((m) => m.id === msgId)
          if (msg) msg.content = this.messageBuffers[msgId]
          this.handlers.onTextMessageContent?.(
            { type: 'TEXT_MESSAGE_CONTENT', messageId: msgId, delta },
            this.messageBuffers[msgId]
          )
        }

        if (!delta && this.chunkCurrentMessageId) {
          const full = this.messageBuffers[this.chunkCurrentMessageId] ?? ''
          delete this.messageBuffers[this.chunkCurrentMessageId]
          const msgEnd = this.messages.find((m) => m.id === this.chunkCurrentMessageId)
          if (msgEnd) msgEnd.content = full
          this.handlers.onTextMessageEnd?.(
            { type: 'TEXT_MESSAGE_END', messageId: this.chunkCurrentMessageId },
            full
          )
          this.chunkCurrentMessageId = null
        }
        break
      }

      case 'TOOL_CALL_START': {
        const e = event as ToolCallStartEvent
        this.toolCallBuffers[e.toolCallId] = {
          name: e.toolCallName,
          argsBuffer: '',
          parentMessageId: e.parentMessageId
        }
        this.handlers.onToolCallStart?.(e)
        break
      }
      case 'TOOL_CALL_ARGS': {
        const e = event as ToolCallArgsEvent
        const buf = this.toolCallBuffers[e.toolCallId]
        if (buf) {
          buf.argsBuffer += e.delta
          this.handlers.onToolCallArgs?.(e, buf.argsBuffer)
        }
        break
      }
      case 'TOOL_CALL_END': {
        const e = event as ToolCallEndEvent
        const bufEnd = this.toolCallBuffers[e.toolCallId]
        if (bufEnd) {
          let parsedArgs: unknown
          try {
            parsedArgs = JSON.parse(bufEnd.argsBuffer)
          } catch {
            parsedArgs = { error: 'Invalid JSON', raw: bufEnd.argsBuffer }
          }
          delete this.toolCallBuffers[e.toolCallId]

          const handler = this.toolHandlers[bufEnd.name]
          if (handler) {
            handler(parsedArgs)
              .then((result) => {
                const toolMsg: Message = {
                  id: `tool_${Date.now()}`,
                  role: 'tool',
                  content:
                    typeof result === 'string' ? result : JSON.stringify(result),
                  toolCallId: e.toolCallId
                }
                this.messages.push(toolMsg)
              })
              .catch((err: unknown) => {
                const errMsg: Message = {
                  id: `tool_${Date.now()}`,
                  role: 'tool',
                  content: `Error: ${err instanceof Error ? err.message : String(err)}`,
                  toolCallId: e.toolCallId
                }
                this.messages.push(errMsg)
              })
          }

          this.handlers.onToolCallEnd?.(e, parsedArgs)
        }
        break
      }
      case 'TOOL_CALL_RESULT': {
        const e = event as ToolCallResultEvent
        const exists = this.messages.some(
          (m) => m.toolCallId === e.toolCallId && m.role === 'tool'
        )
        if (!exists) {
          this.messages.push({
            id: e.messageId,
            role: 'tool',
            content: e.content,
            toolCallId: e.toolCallId
          })
        }
        this.handlers.onToolCallResult?.(e)
        break
      }

      case 'MESSAGES_SNAPSHOT':
        this.messages = (event as MessagesSnapshotEvent).messages
        this.handlers.onMessagesSnapshot?.(event as MessagesSnapshotEvent)
        break
      case 'STATE_SNAPSHOT':
        this.state = (event as StateSnapshotEvent).snapshot
        this.handlers.onStateSnapshot?.(event as StateSnapshotEvent)
        break
      case 'STATE_DELTA': {
        const e = event as StateDeltaEvent
        try {
          const result = applyPatch(
            this.state,
            e.delta as Parameters<typeof applyPatch>[1]
          )
          this.state = result.newDocument
          this.handlers.onStateDelta?.(e)
        } catch (err) {
          console.error('State delta apply failed', err)
        }
        break
      }

      case 'ACTIVITY_SNAPSHOT': {
        const e = event as ActivitySnapshotEvent
        if (!this.activityMessages[e.messageId] || e.replace !== false) {
          this.activityMessages[e.messageId] = {
            id: e.messageId,
            role: 'activity',
            activityType: e.activityType,
            content: e.content
          }
        }
        this.handlers.onActivitySnapshot?.(e)
        break
      }
      case 'ACTIVITY_DELTA': {
        const e = event as ActivityDeltaEvent
        const act = this.activityMessages[e.messageId]
        const content = act?.content
        if (
          act &&
          typeof content === 'object' &&
          content !== null
        ) {
          try {
            const result = applyPatch(
              content,
              e.patch as Parameters<typeof applyPatch>[1]
            )
            act.content = result.newDocument
            this.handlers.onActivityDelta?.(e)
          } catch (err) {
            console.error('Activity delta apply failed', err)
          }
        }
        break
      }

      case 'CUSTOM':
        this.handlers.onCustom?.(event as CustomEvent)
        break
      case 'RAW':
        this.handlers.onRaw?.(event as RawEvent)
        break

      default:
        console.warn('Unknown event type:', event.type)
    }
  }

  /**
   * 中止当前 run 请求
   */
  abort(): void {
    this.abortController?.abort()
    this.abortController = null
  }

  /**
   * 追加一条用户消息
   * @param content 文本内容
   * @param id 可选消息 id
   * @returns 新消息对象
   */
  addUserMessage(content: string, id?: string): Message {
    const msg: Message = {
      id: id ?? `msg_${Date.now()}`,
      role: 'user',
      content
    }
    this.messages.push(msg)
    return msg
  }
}
