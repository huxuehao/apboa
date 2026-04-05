<script setup lang="ts">
import { EllipsisOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  session: { id: string | number; title?: string; isPinned?: boolean;updatedAt: string;createdAt:string }
  active: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'menu', key: string): void
}>()
</script>

<template>
  <div class="chat-history-item" :class="{ active }" @click="emit('click')">
    <span class="chat-history-item-text" :title="session.title || '新对话'">
      {{ session.title || '新对话' }}
    </span>
    <ADropdown :trigger="['click']">
      <AButton type="text" size="small" class="chat-history-item-more" @click.stop>
        <EllipsisOutlined />
      </AButton>
      <template #overlay>
        <AMenu @click="({ key }) => emit('menu', key as string)">
          <AMenuItem key="rename">重命名</AMenuItem>
          <AMenuItem :key="session.isPinned ? 'unpin' : 'pin'">
            {{ session.isPinned ? '取消置顶' : '置顶' }}
          </AMenuItem>
          <AMenuItem key="delete" danger>删除</AMenuItem>
        </AMenu>
      </template>
    </ADropdown>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
