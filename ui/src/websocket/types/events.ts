import type {WS_MESSAGE_TYPES, WSMessageType} from '../const/websocket';

// 消息内容类型映射
export interface MessagePayloadMap {
  // Ping/Pong
  [WS_MESSAGE_TYPES.PING]: string;
  [WS_MESSAGE_TYPES.PONG]: string;

  // 用户相关
  [WS_MESSAGE_TYPES.CLIENT]: string;
  [WS_MESSAGE_TYPES.ACCOUNT_ROLE_CHANGE]: {
    accountId: string;
    role: "ADMIN" | "EDIT" | "READ_ONLY";
  };

  // 默认
  [key: string]: any;
}

// 带payload的消息
export interface TypedWebSocketMessage<T extends WSMessageType = WSMessageType> {
  type: T;
  content?: T extends keyof MessagePayloadMap ? MessagePayloadMap[T] : any;
}

// 事件订阅选项
export interface EventSubscriptionOptions {
  /** 是否只接收最新的一条消息 */
  latest?: boolean;
  /** 过滤器 */
  filter?: (data: any) => boolean;
  /** 错误处理 */
  onError?: (error: Error) => void;
}
