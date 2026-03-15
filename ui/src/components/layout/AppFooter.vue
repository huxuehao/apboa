<script setup lang="ts">
import { GithubOutlined } from '@ant-design/icons-vue'
/**
 * 应用底部组件
 *
 * @author huxuehao
 */
const currentYear = new Date().getFullYear()
</script>

<template>
  <div class="app-footer flex items-center justify-center">
    <div class="footer-content flex flex-col items-center gap-sm">
      <div class="footer-links flex items-center gap-sm">
        <a href="https://github.com/huxuehao/apboa" target="_blank" rel="noopener noreferrer">
          <GithubOutlined style="font-size: 16px; margin-top: 5px" />
        </a>
        <a href="https://gitee.com/studioustiger/apboa" target="_blank" rel="noopener noreferrer">
          <img src="https://gitee.com/static/images/logo-en.svg" width="16" alt="gitee">
        </a>
      </div>

      <div class="footer-description">
        <p>Apboa 基于 AgentScope 框架搭建的智能体平台</p>
      </div>

      <div class="footer-copyright">
        <p>&copy; {{ currentYear }} Apboa. All rights reserved. Powered by Huxuehao</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/components/footer' as *;
</style>
