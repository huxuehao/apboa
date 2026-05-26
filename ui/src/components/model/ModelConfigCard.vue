/**
 * 模型配置卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed, h } from 'vue'
import {
  CheckCircleFilled,
  CloseCircleFilled,
  EllipsisOutlined,
  LoadingOutlined,
  SlackOutlined,
  WarningFilled,
  ApiOutlined
} from '@ant-design/icons-vue'
import type { ModelConfigVO } from '@/types'
import { ModelConnectivityStatus } from '@/types'
import {
  createDeleteItem,
  createDivider,
  createEditItem,
  createEnableItem
} from '@/composables/useCardMenuItems'

const props = defineProps<{
  data: ModelConfigVO
}>()

const emit = defineEmits<{
  edit: [id: string]
  delete: [id: string]
  enable: [id: string]
  test: [id: string]
}>()

/* ============================================================
 * 模型类型显示映射
 * ============================================================ */
const modelTypeLabels: Record<string, string> = {
  CHAT: '文本',
  IMAGE: '图像',
  AUDIO: '音频',
  VIDEO: '视频'
}

/**
 * 格式化更新时间
 */
const formattedTime = computed(() => {
  if (!props.data.updatedAt) return ''
  const date = new Date(props.data.updatedAt)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

/* ============================================================
 * 连接状态角标系统
 * 六态：停用 disabled | 待检测 not-checked | 检测中 checking
 *       | 已连接 connected | 检测失败 failed
 * ============================================================ */

const isDisabled = computed(() => !props.data.enabled)

const isChecking = computed(() => props.data.connectivityStatus === ModelConnectivityStatus.CHECKING)

const isFailed = computed(() => props.data.connectivityStatus === ModelConnectivityStatus.FAILED)

const isConnected = computed(() => props.data.connectivityStatus === ModelConnectivityStatus.CONNECTED)

/** 未检测或默认状态 */
const isNotChecked = computed(
  () => !props.data.connectivityStatus || props.data.connectivityStatus === ModelConnectivityStatus.NOT_CHECKED
)

/** 头像样式类 */
const avatarClass = computed(() => ({
  disabled: isDisabled.value,
  checking: isChecking.value,
  failed: isFailed.value,
  'not-checked': isNotChecked.value,
  connected: isConnected.value && !isDisabled.value
}))

/**
 * 角标提示文本
 */
const cornerTooltip = computed(() => {
  if (isDisabled.value) return '模型已停用'
  if (isChecking.value) return '正在检测连接中，请稍候'
  if (isNotChecked.value) return '尚未进行连接性检测，可通过菜单中的「测试连接」进行检测'
  if (isFailed.value) {
    return props.data.connectivityMessage || '连接检测失败，请检查模型配置或供应商API密钥'
  }
  if (isConnected.value) return '连接正常'
  return '未知状态'
})

/**
 * 角标内容类型
 */
type CornerBadgeType = 'none' | 'warning' | 'error' | 'success'

const cornerBadgeType = computed<CornerBadgeType>(() => {
  if (isDisabled.value || isChecking.value) return 'none'
  if (isFailed.value) return 'error'
  if (isNotChecked.value) return 'warning'
  if (isConnected.value) return 'success'
  return 'none'
})

/**
 * 下拉菜单项
 */
const menuItems = computed(() => {
  return [
    { key: 'test', label: '测试连接', icon: () => h(ApiOutlined) },
    createEditItem(),
    createEnableItem(props.data.enabled),
    createDivider(),
    createDeleteItem()
  ]
})

function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'test':
      emit('test', props.data.id as string)
      break
    case 'edit':
      emit('edit', props.data.id as string)
      break
    case 'enable':
      emit('enable', props.data.id as string)
      break
    case 'delete':
      emit('delete', props.data.id as string)
      break
  }
}

/**
 * 温度参数格式化显示
 */
const formattedTemperature = computed(() => {
  return props.data.temperature?.toFixed(1)
})
</script>

<template>
  <div class="model-config-card">
    <div class="card-header flex items-center gap-sm">
      <div class="card-avatar-wrapper">
        <ATooltip :title="cornerTooltip" placement="top">
          <div class="card-avatar flex-center" :class="avatarClass">
            <LoadingOutlined v-if="isChecking" spin />
            <SlackOutlined v-else />
          </div>
          <!-- 角标 -->
          <span class="avatar-corner-badge" :class="`badge-${cornerBadgeType}`">
            <WarningFilled v-if="cornerBadgeType === 'warning'" />
            <CloseCircleFilled v-else-if="cornerBadgeType === 'error'" />
            <CheckCircleFilled v-else-if="cornerBadgeType === 'success'" />
          </span>
        </ATooltip>
      </div>
      <ATooltip :title="`模型ID：${data.modelId}`" placement="top">
        <div class="card-name flex-1 truncate" style="min-width: 0" :title="data.name" @click="emit('edit', data.id as string)">
            {{ data.name }}
        </div>
      </ATooltip>

      <ADropdown :trigger="['hover']">
        <AButton type="text" size="small" v-permission="['EDIT','ADMIN']">
          <EllipsisOutlined />
        </AButton>
        <template #overlay>
          <AMenu @click="handleMenuClick" :items="menuItems"></AMenu>
        </template>
      </ADropdown>
    </div>

    <div class="card-content line-clamp-2" :title="data.description">
      {{ data.description || '暂无描述' }}
    </div>

    <!-- 参数精简行 -->
    <div class="card-params flex items-center gap-sm text-xs text-placeholder">
      <span>Context: {{ data.contextWindow }}</span>
      <span class="param-divider">|</span>
      <span>Tokens: {{ data.maxTokens }}</span>
      <span class="param-divider">|</span>
      <span>T: {{ formattedTemperature }}</span>
    </div>

    <div class="card-footer flex items-center justify-between">
      <div class="card-tags flex items-center gap-xs">
        <ATag :bordered="false">
            {{ data.streaming ? '流式' : '非流式' }}
        </ATag>
        <ATag :bordered="false">
            {{ data.thinking ? '思考' : '非思考' }}
        </ATag>
      </div>
      <div class="card-tags flex items-center gap-xs">
        <ATag v-for="t in (Array.isArray(data.modelType) ? data.modelType : [data.modelType])"
              :key="t" color="default" class="tag" :bordered="false">
          {{ modelTypeLabels[t] || t }}
        </ATag>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.model-config-card {
  min-height: 180px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-white);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border-radius: var(--border-radius-lg);
  transition: all var(--transition-base);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);

  &:hover {
    box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }

  .card-header {
    .card-avatar-wrapper {
      position: relative;
      flex-shrink: 0;
      cursor: pointer;
    }

    .card-avatar {
      width: 40px;
      height: 40px;
      background-color: #e8f0fe;
      color: #4285f4;
      border-radius: var(--border-radius-xl);
      font-size: var(--font-size-2xl);
      font-weight: 600;
      flex-shrink: 0;
      transition: background-color var(--transition-base), color var(--transition-base);

      /* 停用态 */
      &.disabled {
        color: #757575 !important;
        background-color: #e7e7e7 !important;
      }

      /* 检测中 */
      &.checking {
        background-color: #e3f2fd;
        color: #42a5f5;
      }

      /* 检测失败 */
      &.failed {
        background-color: #ffebee;
        color: #ef5350;
      }

      /* 待检测 */
      &.not-checked {
        background-color: #fff8e1;
        color: #ffa726;
      }

      /* 已连接 */
      &.connected {
        background-color: #e8f5e9;
        color: #52c41a;
      }
    }

    /* 角标 - 定位于头像右下角 */
    .avatar-corner-badge {
      position: absolute;
      bottom: -4px;
      right: -4px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      box-shadow: 0 0 0 2px var(--color-bg-white);
      cursor: pointer;
      font-size: 12px;

      &.badge-warning {
        width: 18px;
        height: 18px;
        background: #fff;
        color: #faad14;
      }

      &.badge-error {
        width: 18px;
        height: 18px;
        background: #fff;
        color: #ff4d4f;
      }

      &.badge-success {
        width: 18px;
        height: 18px;
        background: #fff;
        color: #52c41a;
      }
    }

    .card-name {
      font-size: var(--font-size-base);
      font-weight: 600;
      color: var(--color-text-primary);
      cursor: pointer;
      transition: color var(--transition-base);
    }
  }

  .card-model-id {
    padding: 2px 0;
    color: var(--color-text-secondary, rgba(0, 0, 0, 0.45));
  }

  .card-content {
    font-size: var(--font-size-sm);
    color: var(--color-text-regular);
    line-height: 1.5;
    padding-top: 4px;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-all;
    min-height: 40px;
  }

  .card-params {
    padding: 4px 0;
    color: var(--color-text-secondary, rgba(0, 0, 0, 0.45));

    .param-divider {
      color: var(--color-border-light, rgba(0, 0, 0, 0.06));
    }
  }

  .card-footer {
    padding-top: var(--spacing-xs);
    margin-top: auto;

    .card-tags {
      flex-wrap: wrap;
    }

    .card-time {
      white-space: nowrap;
    }
  }
}
</style>
