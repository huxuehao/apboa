import { useAccountStore } from '@/stores'
import { md5 } from 'js-md5';
import type {AccountVO} from "@/types";

const WS_URL = `ws://${window.location.hostname}:${window.location.port}/apboa/ws`

let reconnectAttempts:number = 0; // 重新连接尝试的次数
let reconnectDelay:number=  45000; // 初始重连延迟 45 秒
let maxReconnectAttempts:number = 1440; // 最大重试次数1440次，约6小时

const accountStore = useAccountStore()

export const initWebSocket = function () {
  const token: string|null = accountStore.accessToken
  if (!token) return

  const socket = new WebSocket(WS_URL);

  // 连接建立时
  socket.onopen = () => {
    reconnectAttempts = 0;
    // 连接建立后发送用户信息
    const userInfo:AccountVO|null = accountStore.userInfo
    const userInfo_ = {
      clientId: md5(token),
      ...userInfo
    }
    socket.send(JSON.stringify({ type: "USER",  content: JSON.stringify(userInfo_)}));
  };

  // 接收到消息时
  socket.onmessage = function (event) {
    const data = JSON.parse(event.data);
    switch (data.type) {
      case "NOTIFY":

        break
      case "PING":
        socket.send(JSON.stringify({ type: "PONG" }));
        break
    }
  };

  socket.onerror = function (error) {
    console.error('WebSocket error:', error);
  };

  // 连接关闭时
  socket.onclose = function () {
    if (reconnectAttempts < maxReconnectAttempts) {
      setTimeout(() => {
        reconnectAttempts++;
        initWebSocket();
      }, reconnectDelay);
    }
  };
}
