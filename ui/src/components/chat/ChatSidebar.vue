<script setup lang="ts">
import { MessageOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import SessionList from './SessionList.vue'

defineProps<{
  collapsed: boolean
  agentName?: string
  pinnedSessions: any[]
  otherSessions: any[]
  currentSessionId: string | null
  userNickname?: string
  isRunning: boolean
  loading: boolean
  hasMore: boolean
  showAccount: boolean
}>()

const emit = defineEmits<{
  (e: 'toggleCollapse'): void
  (e: 'newSession'): void
  (e: 'selectSession', session: any): void
  (e: 'sessionMenu', key: string, session: any): void
  (e: 'loadMore'): void
}>()

/**
 * 检测是否为移动端
 */
const isMobile = () => window.innerWidth <= 768

/**
 * 处理遮罩层点击事件（移动端关闭侧边栏）
 */
const handleMaskClick = () => {
  emit('toggleCollapse')
}

/**
 * 处理选择会话
 */
const handleSelectSession = (session: any) => {
  emit('selectSession', session)
  if (isMobile()) {
    emit('toggleCollapse')
  }
}


/**
 * 处理选择会话
 */
const handleSelect = (k: any, session: any) => {
  emit('sessionMenu', k, session)
  if (isMobile() && ['rename','delete'].includes(k)) {
    emit('toggleCollapse')
  }
}


/**
 * 处理开启新对话
 */
const handleNewSession = () => {
  emit('newSession')
  if (isMobile()) {
    emit('toggleCollapse')
  }
}

</script>

<template>
  <aside class="chat-sidebar" :class="{ collapsed }">
    <!-- 移动端遮罩层（点击关闭侧边栏） -->
    <div
      v-if="!collapsed"
      class="chat-sidebar-mask"
      @click="handleMaskClick"
    />
    <div class="chat-sidebar-header">
      <div class="chat-sidebar-brand">
        <img src="@/assets/images/logo/logo.png" alt="logo" class="chat-sidebar-logo" />
        <span class="chat-sidebar-name" :title="agentName || '智能体'">{{ agentName || '智能体' }}</span>
      </div>
      <AButton type="text" class="chat-sidebar-collapse-btn" :title="collapsed ? '展开' : '折叠'" @click="$emit('toggleCollapse')">
        <MenuFoldOutlined v-if="!collapsed" />
        <MenuUnfoldOutlined v-else />
      </AButton>
    </div>
    <div class="chat-sidebar-body">
      <div v-show="!collapsed" class="chat-sidebar-new-wrap">
        <button type="button" class="chat-sidebar-new-btn" :disabled="isRunning" title="开启新对话" @click="handleNewSession">
          <MessageOutlined /><span>开启新对话</span>
        </button>
      </div>
      <div v-show="collapsed" class="chat-sidebar-new-wrap chat-sidebar-new-wrap-collapsed">
        <AButton
          type="text"
          class="chat-sidebar-collapse-btn"
          :disabled="isRunning"
          title="新对话"
          @click="$emit('newSession')"
        >
          <MessageOutlined />
        </AButton>
      </div>
      <div v-show="!collapsed" class="chat-history-section">
        <SessionList
          :pinned-sessions="pinnedSessions"
          :other-sessions="otherSessions"
          :current-session-id="currentSessionId"
          :loading="loading"
          :has-more="hasMore"
          @select="handleSelectSession"
          @menu="(k, session) => handleSelect(k, session)"
          @load-more="$emit('loadMore')"
        />
      </div>
    </div>
    <div class="chat-sidebar-footer" v-if="showAccount">
      <div class="chat-sidebar-user-avatar" :title="userNickname || '用户'">
        {{ (userNickname || 'U').charAt(0).toUpperCase() }}
      </div>
      <span class="chat-sidebar-user-name" :title="userNickname || '用户'">{{ userNickname || '用户' }}</span>
    </div>
  </aside>
</template>

<style scoped lang="scss">
@use '@/styles/chat/index.scss' as *;
</style>
