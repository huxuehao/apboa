import { createApp, ref, h, defineComponent } from 'vue'
import { Modal } from 'ant-design-vue'
import {
  ZoomInOutlined,
  ZoomOutOutlined,
  RotateLeftOutlined,
  RotateRightOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'

/**
 * 图片预览配置项
 */
interface ImagePreviewOptions {
  /** 图片 URL */
  url: string
  /** 图片标题/文件名 */
  title?: string
}

/**
 * 描述：图片预览 API
 * 用于在任意位置调起图片预览弹窗
 */
export const ImagePreviewApi = {
  /**
   * 打开图片预览
   *
   * @param options 预览配置项
   */
  open(options: ImagePreviewOptions): void {
    const container = document.createElement('div')
    document.body.appendChild(container)

    let destroyed = false

    function destroy() {
      if (destroyed) return
      destroyed = true
      openRef.value = false
    }

    function unmountApp() {
      if (!container.parentNode) return
      appInstance.unmount()
      document.body.removeChild(container)
    }

    const openRef = ref(true)
    const scale = ref(1)
    const rotate = ref(0)

    // 拖拽相关状态
    const translateX = ref(0)
    const translateY = ref(0)
    const isDragging = ref(false)
    const dragStartX = ref(0)
    const dragStartY = ref(0)
    const dragStartTranslateX = ref(0)
    const dragStartTranslateY = ref(0)

    function handleZoomIn() {
      scale.value = Math.min(scale.value + 0.25, 3)
    }

    function handleZoomOut() {
      scale.value = Math.max(scale.value - 0.25, 0.5)
    }

    function handleRotateLeft() {
      rotate.value -= 90
      // 旋转后重置位置，避免偏移
      translateX.value = 0
      translateY.value = 0
    }

    function handleRotateRight() {
      rotate.value += 90
      // 旋转后重置位置，避免偏移
      translateX.value = 0
      translateY.value = 0
    }

    // 拖拽开始
    function onMouseDown(e: MouseEvent) {
      // 只允许在图片上拖拽
      if ((e.target as HTMLElement).tagName !== 'IMG') return

      isDragging.value = true
      dragStartX.value = e.clientX
      dragStartY.value = e.clientY
      dragStartTranslateX.value = translateX.value
      dragStartTranslateY.value = translateY.value

      e.preventDefault()
    }

    // 拖拽中
    function onMouseMove(e: MouseEvent) {
      if (!isDragging.value) return

      const deltaX = e.clientX - dragStartX.value
      const deltaY = e.clientY - dragStartY.value

      translateX.value = dragStartTranslateX.value + deltaX
      translateY.value = dragStartTranslateY.value + deltaY
    }

    // 拖拽结束
    function onMouseUp() {
      isDragging.value = false
    }

    const PreviewComponent = defineComponent({
      name: 'ImagePreviewWrapper',
      setup() {
        return () =>
          h(
            Modal,
            {
              open: openRef.value,
              'onUpdate:open': (val: boolean) => {
                openRef.value = val
              },
              footer: null,
              closable: false,
              maskClosable: true,
              mask: false,
              wrapClassName: 'full-modal image-preview-modal',
              onCancel: destroy,
              onAfterClose: unmountApp,
            },
            {
              default: () =>
                h('div', { class: 'image-preview-container' }, [
                  // 顶部工具栏
                  h('div', { class: 'image-preview-header' }, [
                    h(
                      'div',
                      { class: 'image-preview-title' },
                      options.title || '图片预览'
                    ),
                    h('div', { class: 'image-preview-actions' }, [
                      h(
                        'button',
                        {
                          class: 'image-preview-btn',
                          title: '缩小',
                          onClick: handleZoomOut,
                        },
                        [h(ZoomOutOutlined)]
                      ),
                      h(
                        'button',
                        {
                          class: 'image-preview-btn',
                          title: '放大',
                          onClick: handleZoomIn,
                        },
                        [h(ZoomInOutlined)]
                      ),
                      h(
                        'button',
                        {
                          class: 'image-preview-btn',
                          title: '向左旋转',
                          onClick: handleRotateLeft,
                        },
                        [h(RotateLeftOutlined)]
                      ),
                      h(
                        'button',
                        {
                          class: 'image-preview-btn',
                          title: '向右旋转',
                          onClick: handleRotateRight,
                        },
                        [h(RotateRightOutlined)]
                      ),
                      h(
                        'button',
                        {
                          class: 'image-preview-btn image-preview-btn-close',
                          title: '关闭',
                          onClick: destroy,
                        },
                        [h(CloseOutlined)]
                      ),
                    ]),
                  ]),
                  // 图片内容
                  h('div', { class: 'image-preview-content' }, [
                    h('img', {
                      src: options.url,
                      alt: options.title || '图片',
                      class: 'image-preview-img',
                      style: {
                        transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value}) rotate(${rotate.value}deg)`,
                        transition: isDragging.value ? 'none' : 'transform 0.3s ease',
                        cursor: isDragging.value ? 'grabbing' : 'grab',
                      },
                      onMousedown: onMouseDown,
                      onMousemove: onMouseMove,
                      onMouseup: onMouseUp,
                    }),
                  ]),
                ]),
            }
          )
      },
    })

    const appInstance = createApp(PreviewComponent)
    appInstance.mount(container)

    // 全局监听鼠标事件
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseup', onMouseUp)

    // 清理函数
    const originalUnmount = unmountApp
    window.unmountApp = () => {
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
      originalUnmount()
    }
  },
}

// 添加必要的样式
const style = document.createElement('style')
style.textContent = `
  .image-preview-modal .ant-modal {
    max-width: 100vw;
    top: 0;
    padding-bottom: 0;
  }

  .image-preview-modal .ant-modal-content {
    background: rgba(0, 0, 0, 0.85);
    height: 100vh;
    border-radius: 0;
  }

  .image-preview-modal .ant-modal-body {
    padding: 0;
    height: 100%;
  }

  .image-preview-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    position: relative;
  }

  .image-preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    backdrop-filter: blur(10px);
    z-index: 1;
  }

  .image-preview-title {
    font-size: 16px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  .image-preview-actions {
    display: flex;
    gap: 12px;
  }

  .image-preview-btn {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    color: white;
    width: 32px;
    height: 32px;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    font-size: 16px;
  }

  .image-preview-btn:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(1.05);
  }

  .image-preview-btn:active {
    transform: scale(0.95);
  }

  .image-preview-btn-close:hover {
    background: #ff4d4f;
  }

  .image-preview-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    position: relative;
  }

  .image-preview-img {
    max-width: 90%;
    max-height: 90%;
    object-fit: contain;
    user-select: none;
    -webkit-user-drag: none;
    transition: transform 0.3s ease;
  }
`

document.head.appendChild(style)

// 修复 TypeScript 类型声明
declare global {
  interface Window {
    unmountApp: () => void
  }
}
