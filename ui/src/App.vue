<script setup lang="ts">
import { RouterView } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import {
  useWebSocketEvent,
  useWebSocketSender
} from '@/websocket/composables/useWebSocketEvent';
import { WS_MESSAGE_TYPES } from '@/websocket/const/websocket';
import { useAccountStore } from '@/stores'
import {type AccountVO} from "@/types";
import {useWebSocket} from "@/websocket/useWebSocket.ts";

const { initWS } = useWebSocket();

useWebSocketSender()
initWS()

// 订阅 ACCOUNT_ROLE_CHANGE 消息
useWebSocketEvent(WS_MESSAGE_TYPES.ACCOUNT_ROLE_CHANGE, (message) => {
  const { userInfo, setUserInfo, setRefresh} = useAccountStore()
  if (userInfo?.id !== message.accountId) {
    return
  }

  setUserInfo({
    ...userInfo,
    roles: [message.role]
  } as AccountVO)

  setRefresh()
});
</script>

<template>
  <AConfigProvider
    :locale="zhCN"
    :theme="{
      token: {
        fontFamily: 'AlimamaFangYuan, sans-serif',
      }
     }">
    <RouterView />
  </AConfigProvider>
</template>

<style scoped></style>
