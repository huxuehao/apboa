import { ref, onUnmounted } from 'vue';
import { useAccountStore } from '@/stores';
import { WS_CONFIG } from '@/constants/websocket';
import { ConnectionManager } from '@/utils/websocket/connection-manager';
import { MessageHandler } from '@/utils/websocket/message-handler';
import { ReconnectionManager } from '@/utils/websocket/reconnection-manager';

export function useWebSocket() {
  const accountStore = useAccountStore();
  const isConnected = ref(false);
  const isConnecting = ref(false);
  const error = ref<Error | null>(null);

  // 提前初始化管理器
  const connectionManager = new ConnectionManager((state) => {
    isConnected.value = state.isConnected;
    isConnecting.value = state.isConnecting;
  });

  const reconnectionManager = new ReconnectionManager(
    connectionManager,
    async () => {
      // 重连前确保旧连接已断开
      if (connectionManager.isConnected()) {
        connectionManager.disconnect();
      }
      await initWS();
    }
  );

  let messageHandler: MessageHandler | null = null;

  const initWS = async () => {
    if (!accountStore.accessToken) {
      error.value = new Error('无可用的访问令牌');
      return;
    }

    try {
      isConnecting.value = true;
      error.value = null;

      const socket = await connectionManager.connect(WS_CONFIG.URL);

      // 初始化消息处理器
      messageHandler = new MessageHandler(socket);

      // 设置消息监听
      socket.onmessage = (event) => {
        try {
          messageHandler?.handleMessage(event);
        } catch (err) {
          console.error('处理WebSocket消息失败:', err);
        }
      };

      socket.onerror = handleError;
      socket.onclose = handleClose;

      // 连接成功后发送用户信息
      messageHandler.sendUserInfo(
        accountStore.accessToken,
        accountStore.userInfo
      );

      isConnected.value = true;
    } catch (err) {
      error.value = err as Error;
      console.error('WebSocket链接失败:', err);

      // 连接失败时尝试重连
      if (connectionManager.shouldReconnect()) {
        reconnectionManager.scheduleReconnect();
      }
    } finally {
      isConnecting.value = false;
    }
  };

  const handleError = (event: Event) => {
    console.error('WebSocket 错误:', event);
    error.value = new Error('WebSocket 连接错误');
  };

  const handleClose = () => {
    isConnected.value = false;
    messageHandler = null; // 清理消息处理器

    if (connectionManager.shouldReconnect()) {
      reconnectionManager.scheduleReconnect();
    }
  };

  const disconnectWS = () => {
    // 取消任何待处理的重连
    reconnectionManager.cancelReconnect();
    // 断开连接
    connectionManager.disconnect();
    // 清理状态
    messageHandler = null;
    isConnected.value = false;
    error.value = null;
  };

  // 组件卸载时清理
  onUnmounted(() => {
    disconnectWS();
  });

  return {
    isConnected,
    isConnecting,
    error,
    initWS,
    disconnectWS,
  };
}
