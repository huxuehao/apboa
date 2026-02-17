import type {Ref} from "vue";

export interface FullscreenElement extends HTMLElement {
  requestFullscreen?: () => Promise<void>
  webkitRequestFullscreen?: () => Promise<void>
  msRequestFullscreen?: () => Promise<void>
  mozRequestFullScreen?: () => Promise<void>
}

export interface FullscreenDocument extends Document {
  fullscreenElement?: Element
  webkitFullscreenElement?: Element
  msFullscreenElement?: Element
  mozFullScreenElement?: Element

  exitFullscreen?: () => Promise<void>
  webkitExitFullscreen?: () => Promise<void>
  msExitFullscreen?: () => Promise<void>
  mozCancelFullScreen?: () => Promise<void>

  fullscreenEnabled?: boolean
  webkitFullscreenEnabled?: boolean
  msFullscreenEnabled?: boolean
  mozFullScreenEnabled?: boolean
}

export interface FullscreenOptions {
  navigationUI?: 'auto' | 'hide' | 'show'
}

export interface UseFullscreenReturn {
  isFullscreen: Ref<boolean>
  element: Ref<HTMLElement | null>
  toggleFullscreen: (el?: HTMLElement | null) => Promise<void>
  enterFullscreen: (el?: HTMLElement | null) => Promise<void>
  exitFullscreen: () => Promise<void>
  isSupported: Ref<boolean>
}
