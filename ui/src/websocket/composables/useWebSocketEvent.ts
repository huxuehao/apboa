import { onUnmounted, onMounted, ref, watch, type Ref } from 'vue';
import { eventBus } from '../core/event-bus';
import { WS_MESSAGE_TYPES, type WSMessageType} from '../const/websocket';
import type { MessagePayloadMap, EventSubscriptionOptions } from '../types/events';
import { EnhancedMessageHandler } from '@/websocket/handlers/enhanced-message-handler';
import { useAccountStore } from '@/stores'
import { md5 } from "js-md5";


// 通用订阅Hook
export function useWebSocketEvent<T extends WSMessageType>(
  type: T,
  handler: (data: T extends keyof MessagePayloadMap ? MessagePayloadMap[T] : any) => void,
  options?: EventSubscriptionOptions & {
    /** 自动取消订阅，默认true */
    autoOff?: boolean;
    /** 依赖数组，当依赖变化时重新订阅 */
    deps?: Ref<any>[];
  }
) {
  const isActive = ref(false);
  let unsubscribe: (() => void) | null = null;
  // 用于 latest 模式下防抖，只保留最新一条
  let latestTimer: ReturnType<typeof setTimeout> | null = null;

  const setupSubscription = () => {
    // 取消旧订阅
    unsubscribe?.();

    // 创建包装处理器
    const wrappedHandler = (data: any) => {
      if (!isActive.value) return;

      try {
        // 应用过滤器
        if (options?.filter && !options.filter(data)) {
          return;
        }

        if (options?.latest) {
          // latest语义：当短时间内频繁触发时，只处理最新的一条（防抖）
          if (latestTimer !== null) {
            clearTimeout(latestTimer);
          }
          latestTimer = setTimeout(() => {
            latestTimer = null;
            if (isActive.value) {
              handler(data);
            }
          }, 0);
        } else {
          handler(data);
        }
      } catch (error) {
        options?.onError?.(error as Error);
        console.error(`事件处理失败 [${type}]:`, error);
      }
    };

    // 订阅事件
    unsubscribe = eventBus.on(type, wrappedHandler);
  };

  // 在 onMounted 中订阅，避免 setup 阶段异常时监听器泄漏
  onMounted(() => {
    isActive.value = true;
    setupSubscription();

    // 如果有依赖，监听变化重新订阅
    if (options?.deps?.length) {
      watch(options.deps, () => {
        if (isActive.value) {
          setupSubscription();
        }
      });
    }
  });

  // 组件卸载时取消订阅
  onUnmounted(() => {
    isActive.value = false;
    // 清理延迟器
    if (latestTimer !== null) {
      clearTimeout(latestTimer);
      latestTimer = null;
    }
    if (options?.autoOff !== false) {
      unsubscribe?.();
      unsubscribe = null;
    }
  });

  // 返回控制方法
  return {
    unsubscribe: () => {
      isActive.value = false;
      if (latestTimer !== null) {
        clearTimeout(latestTimer);
        latestTimer = null;
      }
      unsubscribe?.();
      unsubscribe = null;
    },
    isActive
  };
}

// 一次性事件监听
export function useWebSocketEventOnce<T extends WSMessageType>(
  type: T,
  handler: (data: T extends keyof MessagePayloadMap ? MessagePayloadMap[T] : any) => void,
  options?: Omit<EventSubscriptionOptions, 'latest'>
) {
  let registered = false;

  onMounted(() => {
    // 在组件挂载完成后再注册，避免未挂载就触发的情况
    eventBus.once(type, handler);
    registered = true;
  });

  onUnmounted(() => {
    // 防御组件在触发前被卸载，主动清理
    if (registered) {
      eventBus.off(type, handler);
      registered = false;
    }
  });
}

// 响应式数据Hook
export function useWebSocketData<T extends WSMessageType>(
  type: T,
  initialValue?: any,
  options?: EventSubscriptionOptions & {
    /** 转换函数 */
    transform?: (data: any) => any;
  }
) {
  const data = ref(initialValue);
  const error = ref<Error | null>(null);
  const lastUpdate = ref<Date | null>(null);

  useWebSocketEvent(type, (payload) => {
    try {
      const transformed = options?.transform ? options.transform(payload) : payload;
      data.value = transformed;
      lastUpdate.value = new Date();
    } catch (err) {
      error.value = err as Error;
      options?.onError?.(err as Error);
    }
  }, options);

  return {
    data,
    error,
    lastUpdate
  };
}

// 发送消息 Hook——自动监听连接事件获取 handler 引用。
// 使用者无需手动 setHandler，只要确保 useWebSocket 在父级组件中初始化即可。
export function useWebSocketSender() {
  const enhancedMessageHandler = ref<EnhancedMessageHandler | null>(null);
  const isReady = ref(false);

  let unsubConnect: (() => void) | null = null;
  let unsubDisconnect: (() => void) | null = null;

  // 在 onMounted 中订阅连接/断开事件，自动同步 handler
  onMounted(() => {
    const { accessToken } = useAccountStore()
    unsubConnect = eventBus.on('WEBSOCKET:CONNECTED', (data: any) => {
      if (data?.handler) {
        enhancedMessageHandler.value = data.handler;
        isReady.value = true;
        send(WS_MESSAGE_TYPES.CLIENT, md5(accessToken))
      }
    });

    unsubDisconnect = eventBus.on('WEBSOCKET:DISCONNECTED', () => {
      enhancedMessageHandler.value = null;
      isReady.value = false;
    });
  });

  // 组件卸载时清理订阅
  onUnmounted(() => {
    unsubConnect?.();
    unsubDisconnect?.();
    unsubConnect = null;
    unsubDisconnect = null;
  });

  // 也支持手动设置（兴容旧用法）
  const setHandler = (handler: EnhancedMessageHandler | null) => {
    enhancedMessageHandler.value = handler;
    isReady.value = !!handler;
  };

  const send = <T extends WSMessageType>(
    type: T,
    payload?: T extends keyof MessagePayloadMap ? MessagePayloadMap[T] : any
  ) => {
    if (!enhancedMessageHandler.value) {
      console.warn('WebSocket消息处理器未初始化，请确认 WebSocket 已连接');
      return false;
    }
    return enhancedMessageHandler.value.sendMessage(type, payload);
  };

  return {
    setHandler,
    send,
    isReady
  };
}
