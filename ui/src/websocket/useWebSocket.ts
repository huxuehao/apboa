import { ref, onUnmounted } from 'vue';
import { useAccountStore } from '@/stores'
import { WS_CONFIG, type WSMessageType } from './const/websocket';
import { ConnectionManager } from './manager/connection-manager';
import { EnhancedMessageHandler } from './handlers/enhanced-message-handler';
import { ReconnectionManager } from './manager/reconnection-manager';
import { eventBus } from './core/event-bus';
import {md5} from "js-md5";

export function useWebSocket() {
  const accountStore = useAccountStore()
  const isConnected = ref(false);
  const isConnecting = ref(false);
  const error = ref<Error | null>(null);
  const messageHandler = ref<EnhancedMessageHandler | null>(null);

  // 通过 callbacks 统一管理所有 socket 事件，避免外部覆盖 socket 上的事件监听
  const connectionManager = new ConnectionManager({
    onStateChange: (state) => {
      isConnected.value = state.isConnected;
      isConnecting.value = state.isConnecting;
      eventBus.emit('WEBSOCKET:CONNECTION_STATE', state);
    },
    onMessage: (event) => {
      // 消息由 EnhancedMessageHandler 处理并分发到 eventBus
      if (!messageHandler.value) return;
      try {
        messageHandler.value.handleMessage(event);
      } catch (err) {
        console.error('处理WebSocket消息失败:', err);
        eventBus.emit('WEBSOCKET:ERROR', {
          type: 'MESSAGE_HANDLER_ERROR',
          error: err
        });
      }
    },
    onError: (event) => {
      // 已连接后的错误（握手失败的错误由 connect() 的 catch 处理）
      console.error('WebSocket 错误:', event);
      error.value = new Error('WebSocket 连接错误');
      eventBus.emit('WEBSOCKET:ERROR', {
        type: 'CONNECTION_ERROR',
        error: event
      });
    },
    onClose: () => {
      // onClose 只在连接建立后断开时触发，不会与 onerror 导致的重连重复
      messageHandler.value = null;
      eventBus.emit('WEBSOCKET:DISCONNECTED', { timestamp: Date.now() });

      if (connectionManager.shouldReconnect()) {
        reconnectionManager.scheduleReconnect();
      }
    },
  });

  const reconnectionManager = new ReconnectionManager(
    connectionManager,
    async () => {
      await initWS();
    }
  );

  const initWS = async () => {
    if (!accountStore.accessToken) {
      const err = new Error('无可用的访问令牌');
      error.value = err;
      eventBus.emit('WEBSOCKET:ERROR', { type: 'NO_TOKEN', error: err });
      return;
    }

    try {
      error.value = null;

      // connect() 内部已统一绑定 onopen/onerror/onclose/onmessage，无需外部重复绑定
      const socket = await connectionManager.connect(WS_CONFIG.URL);

      // 初始化增强消息处理器（仅持有 socket 引用用于发送消息）
      messageHandler.value = new EnhancedMessageHandler(socket, {
        autoParse: true,
        ignorePingPong: true
      });

      // 发布连接成功事件（携带 handler 引用，供 useWebSocketSender 自动获取）
      eventBus.emit('WEBSOCKET:CONNECTED', {
        timestamp: Date.now(),
        handler: messageHandler.value
      });

    } catch (err) {
      error.value = err as Error;
      console.error('WebSocket连接失败:', err);

      eventBus.emit('WEBSOCKET:ERROR', {
        type: 'CONNECTION_FAILED',
        error: err
      });

      // 连接失败时尝试重连（shouldReconnect 内已判断 manualDisconnect）
      if (connectionManager.shouldReconnect()) {
        reconnectionManager.scheduleReconnect();
      }
    }
  };

  const disconnectWS = () => {
    reconnectionManager.cancelReconnect();
    connectionManager.disconnect(); // 内部会设置 manualDisconnect = true，阻止重连
    messageHandler.value = null;
    error.value = null;

    eventBus.emit('WEBSOCKET:DISCONNECTED', {
      timestamp: Date.now(),
      manual: true
    });
  };

  // 发送消息方法
  const sendMessage = <T extends WSMessageType>(
    type: T,
    payload?: any
  ) => {
    return messageHandler.value?.sendMessage(type, payload) ?? false;
  };

  onUnmounted(() => {
    disconnectWS();
  });

  return {
    isConnected,
    isConnecting,
    error,
    messageHandler,
    initWS,
    disconnectWS,
    sendMessage,
  };
}
