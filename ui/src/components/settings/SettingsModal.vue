/**
 * 系统设置主模态窗组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import {ref, provide} from 'vue'
import SettingsMenu from './SettingsMenu.vue'
import MyAccount from './MyAccount.vue'
import AllAccounts from './AllAccounts.vue'
import SystemIntro from './SystemIntro.vue'

/**
 * 当前选中的菜单项
 */
const currentMenu = ref<string>('myAccount')

/**
 * 提供给子组件的上下文
 */
provide('currentMenu', currentMenu)

/**
 * 定义组件暴露的方法
 */
defineExpose({
  currentMenu
})
</script>

<template>
  <div class="settings-container">
    <div class="settings-sidebar">
      <SettingsMenu v-model="currentMenu" />
    </div>
    <div class="settings-divider"></div>
    <div class="settings-content">
      <MyAccount v-if="currentMenu === 'myAccount'" />
      <AllAccounts v-else-if="currentMenu === 'allAccounts'" v-permission="['EDIT','ADMIN']"/>
      <SystemIntro v-else-if="currentMenu === 'systemIntro'" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;
</style>
