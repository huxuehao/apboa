import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useAgentClient } from '@/composables/useAgentClient'
import * as chatSessionApi from '@/api/chatSession'
import { buildToolCallsContent } from '@/utils/chat/format'
import { chatMessageQueue } from '@/utils/chat/messageQueue'
import type {ChatMessageVO, RawEvent} from '@/types'
import { useAccountStore } from '@/stores'

export function useChatStream(
  agentId: import('vue').Ref<string>,
  agentDetail: import('vue').Ref<any>,
  currentSessionId: import('vue').Ref<string | null>,
  fileIds?: import('vue').Ref<string[]>,
  memoryActive?: import('vue').Ref<boolean>,
  planActive?: import('vue').Ref<boolean>,
  toolProcessActive?: import('vue').Ref<boolean>,
  onMessageSaved?: (chatMsg: ChatMessageVO) => void) {

  const { userInfo } = useAccountStore()

  const getForwardedProps = () => ({
    agentId: agentId.value,
    agentCode: agentDetail.value?.agentCode,
    fileIds: fileIds?.value ?? [],
    memoryActive: memoryActive?.value ?? false,
    planActive: planActive?.value ?? false,
    userInfo: userInfo
  })

  // 流式内容
  const agentHasResult = ref(true)
  const streamingContent = ref('')
  const streamingMessageId = ref<string | null>(null)

  // 推理流式内容
  const reasoningContent = ref('')
  const reasoningMessageId = ref<string | null>(null)

  // 工具调用进度
  const toolCallsInProgress = ref<
    Array<{ id: string; name: string; args: string; result?: string; startTime: number; elapsed?: number, needConfirm?: boolean }>
  >([])

  // 使用原有的 useAgentClient
  const { messages, isRunning, run, abort, addUserMessage, client } = useAgentClient({
    handlers: {
      onRunStarted: () => {
        toolCallsInProgress.value = []
        reasoningContent.value = ''
        reasoningMessageId.value = null
        // 新 run 开始时清理上一轮残留的僵尸队列，防止异常中断留下的任务阻塞后续消息
        const sid = currentSessionId.value
        if (sid) {
          chatMessageQueue.clear(sid)
        }
      },
      onTextMessageStart: (e) => {
        streamingMessageId.value = e.messageId
        streamingContent.value = ''
      },
      onTextMessageContent: (_e, currentText) => {
        agentHasResult.value = true
        streamingContent.value = currentText
      },
      onTextMessageEnd: (_e, finalText) => {
        const sid = currentSessionId.value
        if (sid && finalText) {
          // 纯文本保存，不再与推理打包，通过队列保证写入顺序
          const contentToSave = JSON.stringify({ reasoning: '', content: finalText })
          chatMessageQueue.enqueue(sid, () =>
            chatSessionApi.appendMessage(sid, { role: 'assistant', content: contentToSave }),
            (res) => {
              onMessageSaved?.(res.data.data)
              // 保存完成后清除流式状态，利用 displayMessages 去重避免闪烁
              streamingContent.value = ''
              streamingMessageId.value = null
            }
          )
        }
        reasoningContent.value = ''
      },
      onReasoningMessageStart: (e) => {
        reasoningMessageId.value = e.messageId
        reasoningContent.value = ''
      },
      onReasoningMessageContent: (_e, currentText) => {
        reasoningContent.value = currentText
      },
      onReasoningMessageEnd: () => {
        const sid = currentSessionId.value
        if (sid && reasoningContent.value) {
          // 推理结束时立即保存为独立消息，通过队列保证写入顺序
          const contentToSave = JSON.stringify({ reasoning: reasoningContent.value, content: '' })
          chatMessageQueue.enqueue(sid, () =>
            chatSessionApi.appendMessage(sid, { role: 'assistant', content: contentToSave }),
            (res) => {
              onMessageSaved?.(res.data.data)
              // 保存完成后清除推理状态，利用 displayMessages 去重避免闪烁
              reasoningMessageId.value = null
              reasoningContent.value = ''
            }
          )
        }
      },
      onToolCallStart: (e) => {
        agentHasResult.value = true
        toolCallsInProgress.value = [
          ...toolCallsInProgress.value,
          { id: e.toolCallId, name: e.toolCallName, args: '', startTime: Date.now() }
        ]

        const sid = currentSessionId.value
        if (sid && reasoningContent.value) {
          // 推理结束时保存为独立消息，通过队列保证写入顺序
          const contentToSave = JSON.stringify({ reasoning: reasoningContent.value, content: '' })
          chatMessageQueue.enqueue(sid, () =>
            chatSessionApi.appendMessage(sid, { role: 'assistant', content: contentToSave }),
            (res) => {
              onMessageSaved?.(res.data.data)
              // 保存完成后清除推理状态，利用 displayMessages 去重避免闪烁
              reasoningMessageId.value = null
              reasoningContent.value = ''
            }
          )
        }
      },
      onToolCallArgs: (_e, partialArgs) => {
        const arr = [...toolCallsInProgress.value]
        const last = arr[arr.length - 1]
        if (last) last.args = partialArgs
        toolCallsInProgress.value = arr
      },
      onToolCallResult: (e) => {
        try {
          // 判断是否开启了显示工具调用
          if (!(toolProcessActive?.value ?? true)) {
            return
          }
          // 更新工具调用结果和耗时
          toolCallsInProgress.value = toolCallsInProgress.value.map((t) =>
            t.id === e.toolCallId ? { ...t, result: e.content, elapsed: Date.now() - t.startTime } : t
          )

          // 保存工具调用消息，通过队列保证写入顺序
          const sid = currentSessionId.value
          if (sid) {
            const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
            if (contentToSave) {
              chatMessageQueue.enqueue(sid, () =>
                chatSessionApi.appendMessage(sid, { role: 'tool', content: contentToSave }),
                (res) => onMessageSaved?.(res.data.data)
              )
            }
          }
        } finally {
          // 清空进行中的工具调用（可根据需要保留，此处清空）
          toolCallsInProgress.value = []
        }
      },
      onRunFinished: (_e) => {
        agentHasResult.value = true
        if (toolCallsInProgress.value.length > 0) {
          toolCallsInProgress.value.forEach(item => item.needConfirm = true)
        }
      },
      onRaw: (event) => {
        const e = event as RawEvent
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const rawEvent: any = e.rawEvent
        if(rawEvent.error) {
          streamingMessageId.value = new Date().getTime() + '' + Math.floor(Math.random() * 90000) + 10000
          streamingContent.value = rawEvent.error
          const sid = currentSessionId.value
          if (sid) {
            chatMessageQueue.enqueue(sid, () =>
              chatSessionApi.appendMessage(sid, { role: 'error', content: rawEvent.error }),
              (res) => {
                onMessageSaved?.(res.data.data)
                // 保存完成后清除流式状态，利用 displayMessages 去重避免闪烁
                streamingContent.value = ''
                streamingMessageId.value = null
              }
            )
          }
        }
     }
    }
  })

  // 发送消息
  const sendToolContent = async (value: any) => {
    const {id, name, args, result, content } = value
    client.messages = [{
      id,
      role: 'tool',
      content: JSON.stringify(content),
      toolCallId: content[0].id,
    }]

    // 判断是否开启了显示工具调用
    if ((toolProcessActive?.value ?? true)) {
      const sid = currentSessionId.value as string
      // 保存历史，通过队列保证写入顺序
      const contentToSave = buildToolCallsContent([{ id, name, args, result, elapsed: 0 }])
      if (contentToSave) {
        await chatMessageQueue.enqueue(sid, () =>
          chatSessionApi.appendMessage(sid, { role: 'tool', content: contentToSave }),
          (res) => {
            toolCallsInProgress.value = toolCallsInProgress.value.filter(item => item.id != id)
            onMessageSaved?.(res.data.data)
          }
        )
      }
    }

    await run({
      threadId: currentSessionId.value || undefined,
      runId: `run_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
      forwardedProps: getForwardedProps()
    })
  }

  // 中止运行
  const abortRun = async  () => {
    await abort()
    agentHasResult.value = true
    const sid = currentSessionId.value
    if (sid) {
      // 保存工具调用消息，通过队列保证写入顺序
      if (toolCallsInProgress.value.length > 0) {
        const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
        if (contentToSave) {
          await chatMessageQueue.enqueue(sid, () =>
            chatSessionApi.appendMessage(sid, { role: 'tool', content: contentToSave }),
            (res) => onMessageSaved?.(res.data.data)
          )
        }
      }
      // 保存AI回复消息
      else {
        if (streamingContent.value) {
          // 推理已在 REASONING_MESSAGE_END 中独立保存，此处只保存纯文本
          await chatMessageQueue.enqueue(sid, () =>
            chatSessionApi.appendMessage(sid, { role: 'assistant', content: streamingContent.value }),
            (res) => onMessageSaved?.(res.data.data)
          )
        }
      }

      toolCallsInProgress.value = []
      streamingContent.value = ''
      streamingMessageId.value = null
      reasoningContent.value = ''
      reasoningMessageId.value = null
      isRunning.value = false

    }

  }

  // 发送消息（可选传入 fileIds 覆盖，用于发送时已清空输入框的场景）
  const sendMessage = async (
    inputText: string,
    messagesList: ChatMessageVO[],
    overrideFileIds?: string[]
  ) => {
    const effectiveFileIds = overrideFileIds ?? fileIds?.value ?? []
    if (!agentId.value) return
    if (!inputText.trim() && !effectiveFileIds.length) return
    if (isRunning.value) return
    if (!agentDetail.value?.agentCode) {
      message.error('智能体信息未加载完成，请稍后再试')
      return
    }

    // 构建 client 需要的消息格式
    client.messages = messagesList
      .filter((m) => !['system', 'tool'].includes(m.role))
      .map((m) => ({
        id: String(m.id),
        role: m.role as any,
        content: (m.content || '') as string
      }))

    const forwardedProps = getForwardedProps()
    if (overrideFileIds !== undefined) {
      forwardedProps.fileIds = overrideFileIds
    }

    agentHasResult.value = false
    await run({
      threadId: currentSessionId.value || undefined,
      runId: `run_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
      forwardedProps
    })
  }

  return {
    agentHasResult,
    streamingContent,
    streamingMessageId,
    reasoningContent,
    reasoningMessageId,
    toolCallsInProgress,
    isRunning,
    abortRun,
    sendMessage,
    sendToolContent,
    client, // 如果需要暴露
  }
}
