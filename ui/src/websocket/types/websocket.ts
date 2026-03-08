import type { WSMessageType } from '../const/websocket';

export interface WebSocketMessage {
  type: WSMessageType;
  content?: string;
}

export interface UserInfoMessage {
  clientId: string;
  userInfo: any;
}

export interface WebSocketState {
  socket: WebSocket | null;
  reconnectAttempts: number;
  isConnecting: boolean;
  isConnected: boolean;
}
