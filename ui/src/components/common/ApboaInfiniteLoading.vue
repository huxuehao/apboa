/**
 * InfiniteLoading 统一包装组件
 *
 * 提供全局一致的中文状态提示（加载中、加载完成、空数据、加载失败）
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref } from 'vue'
import { LoadingOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import InfiniteLoading from 'v3-infinite-loading'
import 'v3-infinite-loading/lib/style.css'

defineProps<{
  /** 用于强制重建组件的 key */
  loadingKey: number
}>()

const emit = defineEmits<{
  infinite: [state: { loaded: () => void; complete: () => void; error: () => void }]
}>()

/** 内部重试计数器，点击重试时自增以强制重建 InfiniteLoading */
const retryKey = ref(0)

/**
 * 传递 InfiniteLoading 的 infinite 事件
 */
function onInfinite(state: { loaded: () => void; complete: () => void; error: () => void }) {
  emit('infinite', state)
}

/**
 * 点击重试：递增 retryKey 强制重建 InfiniteLoading 组件
 */
function handleRetry() {
  retryKey.value++
}
</script>

<template>
  <InfiniteLoading
    :key="`${loadingKey}-${retryKey}`"
    @infinite="onInfinite"
  >
    <template #spinner>
      <div class="info-indicator load-indicator mt-md">
        <span class="ml-sm text-secondary">
          <LoadingOutlined style="margin-right: 6px" />加载中
        </span>
      </div>
    </template>

    <template #complete>
      <div class="info-indicator no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>
    </template>

    <template #empty>
      <div class="empty-indicator mt-lg">
        <a-empty description="暂无数据" />
      </div>
    </template>

    <template #error>
      <div class="info-indicator mt-lg">
        <div class="error-content">
          <span class="error-icon">
           <ExclamationCircleOutlined />
          </span>
          <p class="error-text">数据加载失败</p>
          <p class="error-hint">请检查网络连接后重试</p>
          <a-button type="primary" size="small" @click="handleRetry">
            重新加载
          </a-button>
        </div>
      </div>
    </template>
  </InfiniteLoading>
</template>

<style scoped lang="scss">
.info-indicator {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.error-icon {
  font-size: 28px;
  color: #ff4d4f;
  opacity: 0.7;
}

.error-text {
  margin: 0;
  font-size: 14px;
  color: rgba(14, 14, 14, 0.65);
  font-weight: 500;
}

.error-hint {
  margin: 0;
  font-size: 12px;
  color: rgba(14, 14, 14, 0.35);
}
</style>
