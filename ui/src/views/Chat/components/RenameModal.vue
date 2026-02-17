<script setup lang="ts">
defineProps<{
  visible: boolean
  title: string
  confirmLoading: boolean
}>()

defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:title', value: string): void
  (e: 'ok'): void
}>()
</script>

<template>
  <AModal
    :open="visible"
    title="重命名"
    ok-text="确定"
    :confirm-loading="confirmLoading"
    @ok="$emit('ok')"
    @update:open="$emit('update:visible', $event)"
  >
    <AInput
      :value="title"
      @input="$emit('update:title', $event.target.value)"
      placeholder="会话标题"
    />
  </AModal>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
