import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useAgentClient } from '@/composables/useAgentClient'
import * as chatSessionApi from '@/api/chatSession'
import { buildToolCallsContent } from '@/utils/chat/format'
import type { ChatMessageVO } from '@/types'

export function useChatStream(options: {
  agentId: import('vue').Ref<string>
  agentDetail: import('vue').Ref<any>
  currentSessionId: import('vue').Ref<string | null>
  onMessageSaved?: () => void
}) {
  const { agentId, agentDetail, currentSessionId, onMessageSaved } = options

  // 流式内容
  const streamingContent = ref('')
  const streamingMessageId = ref<string | null>(null)

  // 工具调用进度
  const toolCallsInProgress = ref<
    Array<{ id: string; name: string; args: string; result?: string; startTime: number; elapsed?: number, needConfirm?: boolean }>
  >([])

  // 使用原有的 useAgentClient
  const { messages, isRunning, run, abort, addUserMessage, client } = useAgentClient({
    handlers: {
      onRunStarted: () => {
        toolCallsInProgress.value = []
      },
      onTextMessageStart: (e) => {
        streamingMessageId.value = e.messageId
        streamingContent.value = ''
      },
      onTextMessageContent: (_e, currentText) => {
        streamingContent.value = currentText
      },
      onTextMessageEnd: async (_e, finalText) => {
        streamingContent.value = ''
        streamingMessageId.value = null
        if (currentSessionId.value) {
          await chatSessionApi.appendMessage(currentSessionId.value, { role: 'assistant', content: finalText })
          onMessageSaved?.()
        }
      },
      onToolCallStart: (e) => {
        toolCallsInProgress.value = [
          ...toolCallsInProgress.value,
          { id: e.toolCallId, name: e.toolCallName, args: '', startTime: Date.now() }
        ]
      },
      onToolCallArgs: (_e, partialArgs) => {
        const arr = [...toolCallsInProgress.value]
        const last = arr[arr.length - 1]
        if (last) last.args = partialArgs
        toolCallsInProgress.value = arr
      },
      onToolCallResult: async (e) => {
        try {
          // 更新工具调用结果和耗时
          toolCallsInProgress.value = toolCallsInProgress.value.map((t) =>
            t.id === e.toolCallId ? { ...t, result: e.content, elapsed: Date.now() - t.startTime } : t
          )

          // 保存工具调用消息
          if (currentSessionId.value) {
            const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
            if (contentToSave) {
              await chatSessionApi.appendMessage(currentSessionId.value, { role: 'tool', content: contentToSave })
              onMessageSaved?.()
            }
          }
        } finally {
          // 清空进行中的工具调用（可根据需要保留，此处清空）
          toolCallsInProgress.value = []
        }
      },
      onRunFinished: (e) => {
        if (toolCallsInProgress.value.length > 0) {
          toolCallsInProgress.value.forEach(item => item.needConfirm = true)
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

    // 保存历史
    const contentToSave = buildToolCallsContent([{ id, name, args, result, elapsed: 0 }])
    if (contentToSave) {
      await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'tool', content: contentToSave })
      onMessageSaved?.()
    }

    await run({
      threadId: currentSessionId.value || undefined,
      runId: `run_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
      forwardedProps: {
        agentId: agentId.value,
        agentCode: agentDetail.value?.agentCode
      }
    })
  }

  // 中止运行
  const abortRun = async  () => {
    await abort()
    if (currentSessionId.value) {
      // 保存工具调用消息
      if (toolCallsInProgress.value.length > 0) {
        const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
        await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'tool', content: contentToSave })
      }
      // 保存AI回复消息
      else {
        await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'assistant', content: streamingContent.value })
      }

      toolCallsInProgress.value = []
      streamingContent.value = ''
      streamingMessageId.value = null
      isRunning.value = false

      onMessageSaved?.()
    }

  }

  // 发送消息
  const sendMessage = async (inputText: string, messagesList: ChatMessageVO[]) => {
    if (!inputText.trim() || !agentId.value) return
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

    await run({
      threadId: currentSessionId.value || undefined,
      runId: `run_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`,
      forwardedProps: {
        agentId: agentId.value,
        agentCode: agentDetail.value?.agentCode
      }
    })
  }

  return {
    streamingContent,
    streamingMessageId,
    toolCallsInProgress,
    isRunning,
    abortRun,
    sendMessage,
    sendToolContent,
    client, // 如果需要暴露
  }
}
