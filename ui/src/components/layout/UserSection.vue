<script setup lang="ts">
/**
 * 用户信息区域组件
 *
 * @author huxuehao
 */
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RoutePaths } from '@/router/constants.ts'
import { useAccountStore } from '@/stores'
import { UndoOutlined, LogoutOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { Modal } from "ant-design-vue";
import SettingsModal from '@/components/settings/SettingsModal.vue'
import { useWebSocket } from '@/composables/useWebSocket';

const { disconnectWS } = useWebSocket();

const router = useRouter()
const { logout, setRefresh, userInfo} = useAccountStore()

/**
 * 系统设置模态窗显示状态
 */
const settingsVisible = ref(false)

/**
 * 用户信息
 */

/**
 * 用户名首字母(用于头像)
 */
const avatarText = computed(() => {
  if (!userInfo?.username) return '?'
  return userInfo.username.charAt(0).toUpperCase()
})

/**
 * 下拉菜单项
 */
const menuItems = [
  {
    key: 'profile',
    label: '系统设置',
    icon: () => h(SettingOutlined),
  },
  {
    type: 'divider',
  },
  {
    key: 'logout',
    danger: true,
    label: '退出登录',
    icon: () => h(LogoutOutlined),
  },
]

/**
 * 处理菜单点击
 */
const handleMenuClick = async ({ key }: { key: string }) => {
  if (key === 'logout') {
    Modal.confirm({
      title: '确认',
      icon: null,
      content: '确认退出当前系统,是否继续?',
      onOk: async () => {
        await logout()
        disconnectWS()
        await router.push(RoutePaths.LOGIN)
      }
    })
  } else if (key === 'profile') {
    settingsVisible.value = true
  }
}

</script>

<template>
  <div class="user-section flex items-center gap-sm pr-md">
    <AButton style="border: none" shape="circle" type="text" @click="setRefresh" title="刷新">
      <template #icon><UndoOutlined /></template>
    </AButton>

    <ADropdown :trigger="['hover']">
      <div class="user-info flex items-center gap-sm">
        <AAvatar :size="32" style="background-color: var(--color-primary)">
          {{ avatarText }}
        </AAvatar>
      </div>

      <template #overlay>
        <AMenu :items="menuItems" @click="handleMenuClick" />
      </template>
    </ADropdown>

    <!-- 系统设置模态窗 -->
    <AModal
      v-model:open="settingsVisible"
      wrap-class-name="full-modal"
      :footer="null"
      :destroyOnClose="true"
      :width="'100%'"
    >
      <SettingsModal />
    </AModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/components/user-section' as *;
</style>
