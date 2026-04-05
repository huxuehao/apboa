<script setup lang="ts">
import { DeleteOutlined } from '@ant-design/icons-vue'

defineProps<{
  session: {
    id: string | number;
    title?: string;
    createdAt?: any;
    updatedAt?: any;
  }
  active: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'delete'): void
}>()
</script>

<template>
  <div class="chat-history-item" :class="{ active }" @click="emit('click')">
    <span class="chat-history-item-text" :title="session.title || '新对话'">
      <span>
        {{ session.title || '新对话' }}
      </span>
      <br>
      <span v-if="session.updatedAt" class="text-placeholder" style="display: inline-block;margin-top: 5px; font-size: 12px">
        最近变动于 {{ session.updatedAt || '无' }}
      </span>
      <span v-else class="text-placeholder" style="display: inline-block; margin-top: 5px">
        最近变动于 {{ session.createdAt || '无' }}
      </span>

    </span>
    <AButton type="text" size="small" class="chat-history-item-more" @click="emit('delete')">
      <DeleteOutlined style="color: red" />
    </AButton>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
