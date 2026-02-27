<script setup lang="ts">
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import { agentDoTool } from '@/api/agent'

defineProps<{
  id: string,
  name: string
  args?: string
  result?: string
  elapsed?: number
  loading?: boolean
  needConfirm?: boolean
}>()

const emit = defineEmits<{
  (e: 'toolContent', value: any): void
}>()

const foldArgs = ref<boolean>(true)
const doing = ref<boolean>(false)

/** 确认按钮点击（mock） */
const handleConfirm = async (id:string, name: string, args: string | undefined | null) => {
  if (!args) {
    message.warning('参数不完整，稍后再试')
  }

  let args_
  try {
    args_ = JSON.parse(args as string)
  } catch (e) {
    message.warning('参数不完整，稍后再试')
    return
  }

  doing.value = true
  const res = await agentDoTool(name, args_)
  if (res.data.code !== 200) {
    doing.value = false
    return
  }

  const content = [{
      type:"tool_result",
      id,
      name,
      output:[{
          type:"text",
          text: JSON.stringify(res.data.data)
        }]
    }]

  emit('toolContent', {
    id,
    name,
    args,
    result: JSON.stringify(res.data.data),
    content
  })
}

/** 取消按钮点击（mock） */
const handleCancel = (id:string, name: string, args: any) => {
  const content = [{
    type:"tool_result",
    id,
    name,
    output:[{
      type:"text",
      text: `用户本次拒绝了你调用 ${name} 工具的请求`
    }]
  }]

  emit('toolContent', {
    id,
    name,
    args,
    result: `用户本次拒绝了你调用 ${name} 工具的请求`,
    content
  })
}

/*查看参数*/
const handleShowArgs = () => {
  foldArgs.value = !foldArgs.value
}
</script>

<template>
  <div>
    <div class="chat-tool-call" :class="{ 'chat-tool-call--loading': loading }">
      <span class="chat-tool-call-dot"></span>
      <span class="chat-tool-call-label">
      <template v-if="loading">正在调用工具 {{ name }}</template>
      <template v-if="needConfirm && !doing" >
        <div class="chat-tool-call-actions">
          <AButton v-if="args && args !== '{}'"
                   type="link"
                   size="small"
                   @click="handleShowArgs">
            {{ `${foldArgs ? '展开参数' : '折叠参数'}` }}
          </AButton>
          <AButton type="primary" size="small" @click="handleConfirm(id, name, args)">允许</AButton>
          <AButton size="small" @click="handleCancel(id, name, args)">禁止</AButton>
        </div>
      </template>
    </span>
    </div>
    <div class="chat-tool-call" v-if="args && args !== '{}' && !foldArgs && !doing">
      {{ args && args !== '{}' ? args : '无参数' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
