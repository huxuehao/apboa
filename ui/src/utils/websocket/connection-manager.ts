import {WS_CONFIG} from '@/constants/websocket';
import type {WebSocketState} from '@/types/websocket';

export class ConnectionManager {
  private state: WebSocketState = {
    socket: null,
    reconnectAttempts: 0,
    isConnecting: false,
    isConnected: false,
  };

  constructor(
    private onStateChange?: (state: WebSocketState) => void
  ) {}

  getState(): WebSocketState {
    return { ...this.state };
  }

  isConnected(): boolean {
    return this.state.isConnected;
  }

  private updateState(partialState: Partial<WebSocketState>) {
    this.state = { ...this.state, ...partialState };
    this.onStateChange?.(this.state);
  }

  async connect(url: string): Promise<WebSocket> {
    if (this.state.isConnecting) {
      throw new Error('WebSocket is already connecting');
    }

    this.updateState({ isConnecting: true });

    return new Promise((resolve, reject) => {
      try {
        const socket = new WebSocket(url);

        socket.onopen = (event) => {
          this.updateState({
            socket,
            isConnecting: false,
            isConnected: true,
            reconnectAttempts: 0,
          });
          resolve(socket);
        };

        socket.onerror = (error) => {
          this.updateState({ isConnecting: false, isConnected: false });
          reject(error);
        };

        socket.onclose = () => {
          this.updateState({ isConnected: false });
        };

      } catch (error) {
        this.updateState({ isConnecting: false, isConnected: false });
        reject(error);
      }
    });
  }

  disconnect() {
    if (this.state.socket) {
      this.state.socket.close();
      this.updateState({
        socket: null,
        isConnected: false,
        isConnecting: false,
      });
    }
  }

  shouldReconnect(): boolean {
    return this.state.reconnectAttempts < WS_CONFIG.MAX_RECONNECT_ATTEMPTS;
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
