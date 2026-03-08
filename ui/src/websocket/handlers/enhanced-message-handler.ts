import {WS_MESSAGE_TYPES, type WSMessageType} from '../const/websocket';
import { eventBus } from '../core/event-bus';
import type { TypedWebSocketMessage, MessagePayloadMap } from '../types/events';

export class EnhancedMessageHandler {
  constructor(
    private socket: WebSocket,
    private options?: {
      /** 是否自动解析JSON内容 */
      autoParse?: boolean;
      /** 是否忽略PING/PONG消息的发布 */
      ignorePingPong?: boolean;
    }
  ) {}

  handleMessage(event: MessageEvent) {
    try {
      let message: TypedWebSocketMessage;

      // 尝试解析消息
      if (typeof event.data === 'string') {
        message = JSON.parse(event.data);
      } else {
        console.warn('不支持的消息格式:', typeof event.data);
        return;
      }

      // 处理PING/PONG
      if (message.type === WS_MESSAGE_TYPES.PING) {
        this.handlePing();
        if (this.options?.ignorePingPong) return;
      }

      if (message.type === WS_MESSAGE_TYPES.PONG) {
        if (this.options?.ignorePingPong) return;
      }

      // 解析content
      let payload = message.content;
      if (this.options?.autoParse && typeof payload === 'string') {
        try {
          payload = JSON.parse(payload);
        } catch {
          console.warn('消息内容不是有效的JSON字符串，将原样传递');
        }
      }

      // 发布到事件总线
      console.debug(`[WebSocket] 收到消息: ${message.type}`, payload);
      eventBus.emit(message.type, payload);

    } catch (error) {
      console.error('处理WebSocket消息失败:', error);
      eventBus.emit('WEBSOCKET:ERROR', {
        type: 'PARSE_ERROR',
        error,
        rawData: event.data
      });
    }
  }

  sendMessage<T extends WSMessageType>(
    type: T,
    payload?: T extends keyof MessagePayloadMap ? MessagePayloadMap[T] : any
  ) {
    if (this.socket.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket未连接，消息发送失败:', type);
      return false;
    }

    const message: TypedWebSocketMessage<T> = {
      type,
      content: payload
    };

    try {
      this.socket.send(JSON.stringify(message));
      console.debug(`[WebSocket] 发送消息: ${type}`, payload);
      return true;
    } catch (error) {
      console.error(`发送消息失败 [${type}]:`, error);
      return false;
    }
  }

  private handlePing() {
    this.sendMessage(WS_MESSAGE_TYPES.PONG, WS_MESSAGE_TYPES.PONG);
  }
}
