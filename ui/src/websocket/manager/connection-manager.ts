import {WS_CONFIG} from '../const/websocket';
import type {WebSocketState} from '../types/websocket';

// 连接事件回调类型
export interface ConnectionCallbacks {
  /** 连接状态变更 */
  onStateChange?: (state: WebSocketState) => void;
  /** 连接断开（含正常断开和异常断开） */
  onClose?: (event: CloseEvent) => void;
  /** 连接错误（握手阶段的错误） */
  onError?: (event: Event) => void;
  /** 收到消息 */
  onMessage?: (event: MessageEvent) => void;
}

export class ConnectionManager {
  private state: WebSocketState = {
    socket: null,
    reconnectAttempts: 0,
    isConnecting: false,
    isConnected: false,
  };

  // 是否已主动断开，用于区分主动断开和异常断开
  private manualDisconnect = false;

  constructor(private callbacks: ConnectionCallbacks = {}) {}

  getState(): WebSocketState {
    return { ...this.state };
  }

  isConnected(): boolean {
    return this.state.isConnected;
  }

  private updateState(partialState: Partial<WebSocketState>) {
    this.state = { ...this.state, ...partialState };
    this.callbacks.onStateChange?.(this.state);
  }

  async connect(url: string): Promise<WebSocket> {
    if (this.state.isConnecting) {
      throw new Error('WebSocket is already connecting');
    }

    this.manualDisconnect = false;
    this.updateState({ isConnecting: true });

    return new Promise((resolve, reject) => {
      try {
        const socket = new WebSocket(url);

        // 握手成功
        socket.onopen = () => {
          this.updateState({
            socket,
            isConnecting: false,
            isConnected: true,
            reconnectAttempts: 0,
          });
          resolve(socket);
        };

        // 握手阶段的错误（连接建立前触发）
        // 注意：onerror 之后必然触发 onclose，由 onclose 统一处理重连
        socket.onerror = (event) => {
          if (!this.state.isConnected) {
            // 握手阶段失败，reject promise
            this.updateState({ isConnecting: false, isConnected: false });
            this.callbacks.onError?.(event);
            reject(event);
          } else {
            // 已连接后的错误，通知上层，由 onclose 统一处理后续
            this.callbacks.onError?.(event);
          }
        };

        // 统一在 onclose 处理断开逻辑，避免 onerror+onclose 双重触发重连
        socket.onclose = (event) => {
          const wasConnected = this.state.isConnected;
          this.updateState({
            socket: null,
            isConnected: false,
            isConnecting: false,
          });
          // 只有连接建立后断开才通知上层（握手失败已通过 onerror+reject 处理）
          if (wasConnected) {
            this.callbacks.onClose?.(event);
          }
        };

        // 消息统一在 ConnectionManager 内分发，避免外部覆盖 onmessage
        socket.onmessage = (event) => {
          this.callbacks.onMessage?.(event);
        };

      } catch (error) {
        this.updateState({ isConnecting: false, isConnected: false });
        reject(error);
      }
    });
  }

  disconnect() {
    this.manualDisconnect = true;
    if (this.state.socket) {
      this.state.socket.close();
      this.updateState({
        socket: null,
        isConnected: false,
        isConnecting: false,
      });
    }
  }

  /** 是否是主动断开（用于上层判断是否需要重连） */
  isManualDisconnect(): boolean {
    return this.manualDisconnect;
  }

  shouldReconnect(): boolean {
    return !this.manualDisconnect && this.state.reconnectAttempts < WS_CONFIG.MAX_RECONNECT_ATTEMPTS;
  }

  getReconnectDelay(): number {
    return Math.min(
      WS_CONFIG.INITIAL_RECONNECT_DELAY * Math.pow(1.5, this.state.reconnectAttempts),
      WS_CONFIG.MAX_RECONNECT_DELAY
    );
  }

  incrementReconnectAttempts() {
    this.updateState({
      reconnectAttempts: this.state.reconnectAttempts + 1,
    });
  }

  resetReconnectAttempts() {
    this.updateState({ reconnectAttempts: 0 });
  }
}
