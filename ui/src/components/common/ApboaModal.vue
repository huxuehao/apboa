<script setup lang="ts">
import { ref, computed, watch, useSlots } from 'vue'
import { FullscreenOutlined, FullscreenExitOutlined, CloseOutlined } from '@ant-design/icons-vue'

/**
 * 描述：将宽度值（字符串或数字）转为像素数值，百分比返回 Infinity
 */
function toPixelValue(val: string | number): number {
  if (val === '100%' || val === 1) return Infinity
  if (typeof val === 'number') return val
  if (val.endsWith('%')) return Infinity
  return parseFloat(val)
}

/**
 * 描述：将宽度值规范为 CSS 字符串
 */
function toCssWidth(val: string | number): string {
  if (typeof val === 'number') return `${val}px`
  return val
}

/**
 * 组件属性定义
 */
const props = withDefaults(
  defineProps<{
    /** 控制显示状态 */
    open?: boolean
    /** 标题 */
    title?: string
    /** 内容区域默认宽度 */
    defaultWidth?: string | number
    /** 点击扩展后的宽度 */
    expandedWidth?: string | number
    /** 默认是否展开为扩展宽度 */
    defaultExpanded?: boolean
    /** 确认按钮 loading */
    confirmLoading?: boolean
    /** 关闭时销毁内容 */
    destroyOnClose?: boolean
    /** 底部区域（null 隐藏，undefined 显示默认按钮） */
    footer?: null | unknown
    /** 确认按钮文字 */
    okText?: string
    /** 取消按钮文字 */
    cancelText?: string
    /** 确认按钮类型 */
    okType?: 'primary' | 'default' | 'dashed' | 'link' | 'text'
    /** 确认按钮附加 props */
    okButtonProps?: Record<string, unknown>
    /** 取消按钮附加 props */
    cancelButtonProps?: Record<string, unknown>
  }>(),
  {
    open: false,
    defaultWidth: '780px',
    expandedWidth: '100%',
    defaultExpanded: false,
    confirmLoading: false,
    destroyOnClose: false,
    okText: '确定',
    cancelText: '取消',
    okType: 'primary',
  }
)

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  'update:open': [value: boolean]
  ok: []
  cancel: []
  afterClose: []
}>()

const slots = useSlots()

// 是否已展开
const isExpanded = ref(props.defaultExpanded)

// 当 expandedWidth 的像素值 <= defaultWidth 的像素值时，扩展按钮无效
const canExpand = computed(() => {
  const expanded = toPixelValue(props.expandedWidth ?? '100%')
  const def = toPixelValue(props.defaultWidth ?? '780px')
  return expanded > def
})

// 当前中间区域的 CSS 宽度
const centerWidth = computed(() =>
  isExpanded.value
    ? toCssWidth(props.expandedWidth ?? '100%')
    : toCssWidth(props.defaultWidth ?? '780px')
)

// 是否有自定义 footer slot
const hasFooterSlot = computed(() => !!slots.footer)

/**
 * 关闭弹窗
 */
function handleClose() {
  emit('update:open', false)
  emit('cancel')
}

/**
 * 点击确认按钮
 */
function handleOk() {
  emit('ok')
}

/**
 * 扩展/收缩切换
 */
function toggleExpand() {
  if (!canExpand.value) return
  isExpanded.value = !isExpanded.value
}

// 当 open 变化时，若关闭则重置展开状态
watch(
  () => props.open,
  (val) => {
    if (!val) {
      isExpanded.value = props.defaultExpanded
    }
  }
)
</script>

<template>
  <AModal
    :open="open"
    :mask="false"
    :closable="false"
    :keyboard="false"
    :footer="null"
    :width="'100vw'"
    :destroy-on-close="destroyOnClose"
    wrap-class-name="apboa-modal-wrap"
    @update:open="emit('update:open', $event)"
    @after-close="emit('afterClose')"
  >
    <!-- 全屏三栏容器 -->
    <div class="apboa-fullscreen">
      <!-- 左侧留白（高斯模糊） -->
      <div class="apboa-side apboa-side-left" />

      <!-- 中间内容区域 -->
      <div class="apboa-center" :style="{ width: centerWidth }">
        <!-- header -->
        <div class="apboa-center-header">
          <div class="apboa-center-header-title">
            <slot name="title">
              <span>{{ title }}</span>
            </slot>
          </div>
          <div class="apboa-center-header-actions">
            <!-- 额外操作区（slot） -->
            <slot name="extra" />
            <!-- 扩展/收缩按钮 -->
            <AButton
              v-if="canExpand"
              type="text"
              class="apboa-icon-btn"
              :title="isExpanded ? '收缩' : '扩展'"
              @click="toggleExpand"
            >
              <template #icon>
                <FullscreenExitOutlined v-if="isExpanded" />
                <FullscreenOutlined v-else />
              </template>
            </AButton>
            <!-- 关闭按钮 -->
            <AButton type="text" class="apboa-icon-btn" title="关闭" @click="handleClose">
              <template #icon>
                <CloseOutlined />
              </template>
            </AButton>
          </div>
        </div>

        <!-- body：内容溢出时滚动，内容不足时垂直居中 -->
        <div class="apboa-center-body">
          <div class="apboa-body-inner">
            <slot v-if="!destroyOnClose || open" />
          </div>
        </div>

        <!-- footer -->
        <div v-if="props.footer !== null" class="apboa-center-footer">
          <slot v-if="hasFooterSlot" name="footer" />
          <template v-else>
            <AButton v-bind="cancelButtonProps" @click="handleClose">
              {{ cancelText }}
            </AButton>
            <AButton
              :type="okType"
              :loading="confirmLoading"
              v-bind="okButtonProps"
              @click="handleOk"
            >
              {{ okText }}
            </AButton>
          </template>
        </div>
      </div>

      <!-- 右侧留白（高斯模糊） -->
      <div class="apboa-side apboa-side-right" />
    </div>
  </AModal>
</template>

<!-- 不使用 scoped，需要穿透 AModal 宿主节点 -->
<style lang="scss">
/* ===================== AModal 宿主重置 ===================== */
.apboa-modal-wrap {
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.65);
  transition: flex 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  /* 对话框容器铺满全屏 */
  .ant-modal {
    top: 0 !important;
    padding: 0 !important;
    margin: 0 !important;
    width: 100vw !important;
    max-width: 100vw !important;
    height: 100vh !important;

    .ant-modal-content {
      padding: 0;
      border-radius: 0;
      background: transparent;
      box-shadow: none;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .ant-modal-body {
      padding: 0;
      flex: 1;
      overflow: hidden;
    }
  }
}

/* ===================== 全屏三栏布局 ===================== */
.apboa-fullscreen {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* 左右留白：高斯模糊 */
.apboa-side {
  flex: 1;
  min-width: 0;
}

/* 中间内容区域 */
.apboa-center {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

/* ===================== Header ===================== */
.apboa-center-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
}

.apboa-center-header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.apboa-center-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.apboa-icon-btn.ant-btn-text {
  color: var(--ant-color-text-secondary, rgba(0, 0, 0, 0.45));
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
    background: var(--ant-color-fill-secondary, rgba(0, 0, 0, 0.06));
  }

  .anticon {
    font-size: 16px;
  }
}

/* ===================== Body ===================== */
.apboa-center-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.apboa-body-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 18px;
}

/* ===================== Footer ===================== */
.apboa-center-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
}
</style>
