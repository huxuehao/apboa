<script setup lang="ts">
import { RouterView } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
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
