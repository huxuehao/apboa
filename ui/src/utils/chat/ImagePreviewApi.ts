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

    function handleZoomIn() {
      scale.value = Math.min(scale.value + 0.25, 3)
    }

    function handleZoomOut() {
      scale.value = Math.max(scale.value - 0.25, 0.5)
    }

    function handleRotateLeft() {
      rotate.value -= 90
    }

    function handleRotateRight() {
      rotate.value += 90
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
                        transform: `scale(${scale.value}) rotate(${rotate.value}deg)`,
                        transition: 'transform 0.3s ease',
                      },
                      onClick: (e: MouseEvent) => e.stopPropagation(),
                    }),
                  ]),
                ]),
            }
          )
      },
    })

    const appInstance = createApp(PreviewComponent)
    appInstance.mount(container)
  },
}
