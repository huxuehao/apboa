import { createApp, defineComponent, ref, h, type VNode } from 'vue'
import Antd from 'ant-design-vue'
import ApboaModal from './ApboaModal.vue'

/**
 * 描述：ApboaModalApi.open() 的配置选项，兼容 ApboaModal 所有 props
 */
export interface ApboaModalApiOptions {
  /** 标题 */
  title?: string
  /** 标题图标组件 */
  titleIcon?: unknown
  /** 内容区域默认宽度 */
  defaultWidth?: string | number
  /** 点击扩展后的宽度 */
  expandedWidth?: string | number
  /** 默认是否展开 */
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
  /** 弹窗内容：VNode 或返回 VNode 的函数 */
  content?: VNode | (() => VNode)
  /** 点击确认的回调 */
  onOk?: () => void | Promise<void>
  /** 点击取消/关闭的回调 */
  onCancel?: () => void
}

/**
 * 描述：ApboaModalApi.open() 返回的实例，可用于手动关闭弹窗
 */
export interface ApboaModalApiInstance {
  /** 手动关闭弹窗 */
  close(): void
}

/**
 * 描述：API 式调用 ApboaModal 的工具类，使用方式类似 antdv 的 Modal.info()
 *
 * @example
 * const modal = ApboaModalApi.open({
 *   title: '详情',
 *   footer: null,
 *   content: h('div', '这是内容'),
 * })
 * // 手动关闭
 * modal.close()
 */
export const ApboaModalApi = {
  /**
   * 打开一个 ApboaModal 弹窗
   *
   * @param options 弹窗配置项
   * @return 弹窗实例，包含 close() 方法
   */
  open(options: ApboaModalApiOptions): ApboaModalApiInstance {
    const container = document.createElement('div')
    document.body.appendChild(container)

    // 标记是否已销毁，防止重复调用
    let destroyed = false

    function destroy() {
      if (destroyed) return
      destroyed = true
      // 先关闭弹窗，等 afterClose 动画结束后再 unmount
      openRef.value = false
    }

    function unmountApp() {
      if (!container.parentNode) return
      appInstance.unmount()
      document.body.removeChild(container)
    }

    // 通过 ref 控制 open 状态，供包装组件使用
    const openRef = ref(true)

    /**
     * 内部包装组件，将 options 映射到 ApboaModal 的 props 和 slots
     */
    const WrapperComponent = defineComponent({
      name: 'ApboaModalApiWrapper',
      setup() {
        return () => {
          const { content, onOk, onCancel, ...modalProps } = options

          return h(
            ApboaModal,
            {
              ...modalProps,
              open: openRef.value,
              'onUpdate:open': (val: boolean) => {
                openRef.value = val
              },
              onOk: onOk,
              onCancel: () => {
                onCancel?.()
              },
              onAfterClose: unmountApp,
            },
            {
              default: () => {
                if (!content) return null
                return typeof content === 'function' ? content() : content
              },
            },
          )
        }
      },
    })

    const appInstance = createApp(WrapperComponent)
    // 注册 antdv，保证 AModal/AButton 等组件可被识别
    appInstance.use(Antd)
    appInstance.mount(container)

    return {
      close: destroy,
    }
  },
}
