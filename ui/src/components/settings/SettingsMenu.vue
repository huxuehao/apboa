/**
 * 系统设置左侧菜单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed } from 'vue'
import { UserOutlined, TeamOutlined, InfoCircleOutlined, DatabaseOutlined, FolderOutlined, ClockCircleOutlined, ControlOutlined } from '@ant-design/icons-vue'
import { useAccountStore } from '@/stores'

/**
 * 定义 props
 */
defineProps<{
  modelValue: string
}>()

/**
 * 定义 emits
 */
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const accountStore = useAccountStore()

/**
 * 是否为管理员
 */
const isAdmin = computed(() => accountStore.isAdmin)

/**
 * 菜单项配置
 */
const menuItems = computed(() => {
  const items = [
    {
      key: 'myAccount',
      label: '我的账户',
      icon: UserOutlined
    },
    {
      key: 'systemIntro',
      label: '系统介绍',
      icon: InfoCircleOutlined
    }
  ]

  if (isAdmin.value) {
    items.splice(1, 0, {
      key: 'allAccounts',
      label: '全部账户',
      icon: TeamOutlined
    })
    items.splice(2, 0, {
      key: 'storageConfig',
      label: '存储配置',
      icon: DatabaseOutlined
    })
    items.splice(3, 0, {
      key: 'fileManager',
      label: '文件管理',
      icon: FolderOutlined
    })
    items.splice(4, 0, {
      key: 'fileLog',
      label: '文件日志',
      icon: ClockCircleOutlined
    })
    items.splice(5, 0, {
      key: 'systemParams',
      label: '系统参数',
      icon: ControlOutlined
    })
  }

  return items
})

/**
 * 处理菜单点击
 */
function handleMenuClick(key: string) {
  emit('update:modelValue', key)
}
</script>

<template>
  <div class="settings-menu">
    <div class="settings-menu-title">系统设置</div>
    <div class="settings-menu-list">
      <div
        v-for="item in menuItems"
        :key="item.key"
        class="settings-menu-item"
        :class="{ active: modelValue === item.key }"
        @click="handleMenuClick(item.key)"
      >
        <component :is="item.icon" class="settings-menu-icon" />
        <span class="settings-menu-label">{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;
</style>
