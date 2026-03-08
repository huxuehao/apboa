import { ConnectionManager } from './connection-manager';

export class ReconnectionManager {
  private reconnectTimer?: number;

  constructor(
    private connectionManager: ConnectionManager,
    private onReconnect: () => Promise<void>
  ) {}

  scheduleReconnect() {
    if (!this.connectionManager.shouldReconnect()) {
      console.log('达到最大尝试连接次数');
      return;
    }

    const delay = this.connectionManager.getReconnectDelay();

    this.clearReconnectTimer();

    this.reconnectTimer = window.setTimeout(async () => {
      this.connectionManager.incrementReconnectAttempts();

      // 确保重连前连接已断开
      if (this.connectionManager.isConnected()) {
        this.connectionManager.disconnect();
      }

      await this.onReconnect();
    }, delay);
  }

  cancelReconnect() {
    this.clearReconnectTimer();
    this.connectionManager.resetReconnectAttempts();
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
  }
}
