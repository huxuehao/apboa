<template>
  <Suspense>
    <template #default>
      <div v-if="errorInfo" class="loading-container">
        <div class="loading-spinner" >
          {{errorInfo}}
        </div>
      </div>
      <ChatWrapper v-else @error="handleError" />
    </template>
    <template #fallback>
      <div class="loading-container">
        <div class="loading-spinner">
          <LoadingOutlined style="margin-right: 6px" />加载中
        </div>
      </div>
    </template>
  </Suspense>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { LoadingOutlined } from "@ant-design/icons-vue"
import ChatWrapper from './ChatWrapper.vue'

const errorInfo = ref<string>('')

const handleError = (error: string) => {
  errorInfo.value = error
}
</script>

<style scoped lang="scss">
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;

  .loading-spinner {
    font-size: 16px;
    color: #666;
  }
}
</style>
