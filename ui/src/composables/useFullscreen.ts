import { ref, onMounted, onUnmounted, type Ref, computed } from 'vue'
import type { UseFullscreenReturn } from '@/types/fullscreen'

export function useFullscreen(target: Ref<HTMLElement | null>): UseFullscreenReturn {
  const isFullscreen = ref<boolean>(false)
  const isSupported = ref<boolean>(true) // 模拟全屏始终支持
  const originalStyles = ref<{
    position: string
    top: string
    left: string
    width: string
    height: string
    zIndex: string
    overflow: string
    backgroundColor: string
  } | null>(null)

  // 保存原始样式
  const saveOriginalStyles = (): void => {
    if (!target.value) return

    originalStyles.value = {
      position: target.value.style.position,
      top: target.value.style.top,
      left: target.value.style.left,
      width: target.value.style.width,
      height: target.value.style.height,
      zIndex: target.value.style.zIndex,
      overflow: target.value.style.overflow,
      backgroundColor: target.value.style.backgroundColor
    }
  }

  // 恢复原始样式
  const restoreOriginalStyles = (): void => {
    if (!target.value || !originalStyles.value) return

    const styles = originalStyles.value
    target.value.style.position = styles.position
    target.value.style.top = styles.top
    target.value.style.left = styles.left
    target.value.style.width = styles.width
    target.value.style.height = styles.height
    target.value.style.zIndex = styles.zIndex
    target.value.style.overflow = styles.overflow
    target.value.style.backgroundColor = styles.backgroundColor
  }

  // 进入模拟全屏
  const enterFullscreen = async (): Promise<void> => {
    if (!target.value) {
      throw new Error('目标元素不存在')
    }

    try {
      saveOriginalStyles()

      // 应用全屏样式
      target.value.style.position = 'fixed'
      target.value.style.top = '0'
      target.value.style.left = '0'
      target.value.style.width = '100vw'
      target.value.style.height = '100vh'
      target.value.style.zIndex = '9999' // 确保在最上层
      target.value.style.overflow = 'auto'
      target.value.style.backgroundColor = '#ffffff' // 默认白色背景

      // 防止body滚动
      document.body.style.overflow = 'hidden'

      isFullscreen.value = true
    } catch (error) {
      console.error('进入全屏失败:', error)
      throw error
    }
  }

  // 退出模拟全屏
  const exitFullscreen = async (): Promise<void> => {
    if (!target.value) {
      throw new Error('目标元素不存在')
    }

    try {
      restoreOriginalStyles()

      // 恢复body滚动
      document.body.style.overflow = ''

      isFullscreen.value = false
    } catch (error) {
      console.error('退出全屏失败:', error)
      throw error
    }
  }

  // 切换全屏
  const toggleFullscreen = async (): Promise<void> => {
    if (isFullscreen.value) {
      await exitFullscreen()
    } else {
      await enterFullscreen()
    }
  }

  // 监听键盘 ESC 退出全屏
  const handleKeydown = (event: KeyboardEvent): void => {
    if (event.key === 'Escape' && isFullscreen.value) {
      exitFullscreen()
    }
  }

  // 监听窗口变化，调整尺寸
  const handleResize = (): void => {
    if (isFullscreen.value && target.value) {
      target.value.style.width = '100vw'
      target.value.style.height = '100vh'
    }
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    // 组件卸载时退出全屏
    if (isFullscreen.value) {
      exitFullscreen()
    }

    window.removeEventListener('resize', handleResize)
    document.removeEventListener('keydown', handleKeydown)
  })

  return {
    isFullscreen,
    element: target,
    toggleFullscreen,
    enterFullscreen,
    exitFullscreen,
    isSupported: computed(() => isSupported.value) // 包装为计算属性保持接口一致
  }
}
