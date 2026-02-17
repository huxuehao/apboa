import type { AccountVO } from './index';
import type { WSMessageType } from '@/constants/websocket';

export interface WebSocketMessage {
  type: WSMessageType;
  content?: string;
}

export interface UserInfoMessage {
  clientId: string;
  userInfo: AccountVO | null;
}

export interface WebSocketState {
  socket: WebSocket | null;
  reconnectAttempts: number;
  isConnecting: boolean;
  isConnected: boolean;
}
