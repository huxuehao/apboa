import { WS_MESSAGE_TYPES } from '@/constants/websocket';
import type { WebSocketMessage, UserInfoMessage } from '@/types/websocket';
import {type AccountVO, Role} from '@/types';
import { md5 } from 'js-md5';
import { useAccountStore } from '@/stores'

export class MessageHandler {
  constructor(
    private socket: WebSocket
  ) {}

  handleMessage(event: MessageEvent) {
    try {
      const data = JSON.parse(event.data) as WebSocketMessage;

      switch (data.type) {
        case WS_MESSAGE_TYPES.ACCOUNT_ROLE_CHANGE:
          this.handleUserRoleChange(data.content);
          break;
        case WS_MESSAGE_TYPES.PING:
          this.handlePing();
          break;
        default:
          console.warn('不支持的消息类型:', data.type);
      }
    } catch (error) {
      console.error('解析 WebSocket 消息失败:', error);
    }
  }

  sendUserInfo(token: string, userInfo: AccountVO | null) {
    const userInfoMessage = {
      clientId: md5(token),
      ...userInfo,
    };

    this.sendMessage({
      type: WS_MESSAGE_TYPES.USER,
      content: JSON.stringify(userInfoMessage),
    });
  }

  private sendMessage(message: WebSocketMessage) {
    if (this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }

  private handleUserRoleChange(content?: any) {
    if (content) {
      const { userInfo, setUserInfo, setRefresh} = useAccountStore()
      if (userInfo?.id !== content?.accountId) {
        return
      }

      setUserInfo({
        ...userInfo,
        roles: [content.role as Role]
      } as AccountVO)

      setRefresh()
    }
  }

  private handlePing() {
    this.sendMessage({ type: WS_MESSAGE_TYPES.PONG, content: WS_MESSAGE_TYPES.PONG });
  }
}
