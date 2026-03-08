export const WS_CONFIG = {
  URL: `ws://${window.location.hostname}:${window.location.port}/api/ws/apboa`,
  MAX_RECONNECT_ATTEMPTS: 1440, // 约6小时
  INITIAL_RECONNECT_DELAY: 45000, // 45秒
  MAX_RECONNECT_DELAY: 300000, // 最大重连延迟5分钟
  PING_INTERVAL: 30000, // 30秒发送一次ping
  PONG_TIMEOUT: 5000, // 等待pong响应超时时间
};

export const WS_MESSAGE_TYPES = {
  CLIENT: 'CLIENT',
  ACCOUNT_ROLE_CHANGE: 'ACCOUNT_ROLE_CHANGE',
  PING: 'PING',
  PONG: 'PONG',
} as const;

export type WSMessageType = typeof WS_MESSAGE_TYPES[keyof typeof WS_MESSAGE_TYPES];
