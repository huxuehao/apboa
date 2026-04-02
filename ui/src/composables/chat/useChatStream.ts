import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useAgentClient } from '@/composables/useAgentClient'
import * as chatSessionApi from '@/api/chatSession'
import { buildToolCallsContent } from '@/utils/chat/format'
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
        agentHasResult.value = true
        streamingContent.value = currentText
      },
      onTextMessageEnd: async (_e, finalText) => {
        streamingContent.value = ''
        streamingMessageId.value = null
        if (currentSessionId.value && finalText) {
          const res = await chatSessionApi.appendMessage(currentSessionId.value, { role: 'assistant', content: finalText })
          onMessageSaved?.(res.data.data)
        }
      },
      onToolCallStart: (e) => {
        agentHasResult.value = true
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
          // 判断是否开启了显示工具调用
          if (!(toolProcessActive?.value ?? true)) {
            return
          }
          // 更新工具调用结果和耗时
          toolCallsInProgress.value = toolCallsInProgress.value.map((t) =>
            t.id === e.toolCallId ? { ...t, result: e.content, elapsed: Date.now() - t.startTime } : t
          )

          // 保存工具调用消息
          if (currentSessionId.value) {
            const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
            if (contentToSave) {
              const res = await chatSessionApi.appendMessage(currentSessionId.value, { role: 'tool', content: contentToSave })
              onMessageSaved?.(res.data.data)
            }
          }
        } finally {
          // 清空进行中的工具调用（可根据需要保留，此处清空）
          toolCallsInProgress.value = []
        }
      },
      onRunFinished: (e) => {
        streamingContent.value = ''
        streamingMessageId.value = null
        agentHasResult.value = true
        if (toolCallsInProgress.value.length > 0) {
          toolCallsInProgress.value.forEach(item => item.needConfirm = true)
        }
      },
      onRaw: async (event) => {
        const e = event as RawEvent
        const rawEvent: any = e.rawEvent
        if(rawEvent.error) {
          streamingMessageId.value = new Date().getTime() + '' + Math.floor(Math.random() * 90000) + 10000
          streamingContent.value = rawEvent.error
          if (currentSessionId.value) {
            const res = await chatSessionApi.appendMessage(currentSessionId.value, { role: 'error', content: rawEvent.error })
            onMessageSaved?.(res.data.data)
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
      // 保存历史
      const contentToSave = buildToolCallsContent([{ id, name, args, result, elapsed: 0 }])
      if (contentToSave) {
        const res = await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'tool', content: contentToSave })
        toolCallsInProgress.value = toolCallsInProgress.value.filter(item => item.id != id)
        onMessageSaved?.(res.data.data)
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
    if (currentSessionId.value) {
      let chatMsg:ChatMessageVO | null = null;
      // 保存工具调用消息
      if (toolCallsInProgress.value.length > 0) {
        const contentToSave = buildToolCallsContent(toolCallsInProgress.value)
        if (contentToSave) {
          const res = await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'tool', content: contentToSave })
          chatMsg = res.data.data
        }
      }
      // 保存AI回复消息
      else {
        if (streamingContent.value) {
          const res = await chatSessionApi.appendMessage(currentSessionId.value as string, { role: 'assistant', content: streamingContent.value })
          chatMsg = res.data.data
        }
      }

      toolCallsInProgress.value = []
      streamingContent.value = ''
      streamingMessageId.value = null
      isRunning.value = false

      if (chatMsg) {
        onMessageSaved?.(chatMsg)
      }
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
    toolCallsInProgress,
    isRunning,
    abortRun,
    sendMessage,
    sendToolContent,
    client, // 如果需要暴露
  }
}
