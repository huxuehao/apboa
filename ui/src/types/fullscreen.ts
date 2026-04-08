import type {Ref} from "vue";

export interface UseFullscreenReturn {
  isFullscreen: Ref<boolean>
  element: Ref<HTMLElement | null>
  toggleFullscreen: (el?: HTMLElement | null) => Promise<void>
  enterFullscreen: (el?: HTMLElement | null) => Promise<void>
  exitFullscreen: () => Promise<void>
  isSupported: Ref<boolean>
}
