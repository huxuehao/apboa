<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
/**
 * 主布局组件
 *
 * @author huxuehao
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
const route = useRoute();
import { useAccountStore } from "@/stores";
import { AppLogo, AppMenu, UserSection, AppFooter } from '@/components/layout'
const { getRefresh } =  useAccountStore()

let isRefresh = computed(() => {
  return getRefresh()
})
</script>

<template>
  <div class="app-layout flex flex-col">
    <header class="layout-header flex items-center">
      <AppLogo project-name="Apboa" />
      <AppMenu />
      <UserSection />
    </header>

    <main class="layout-content flex-1">
      <router-view
        v-slot="{ Component }"
        :key="route.path + isRefresh"
      >
        <transition name="slide-right" mode="out-in" appear>
          <div style="height: 100%; position: relative;">
            <component :is="Component" />
          </div>
        </transition>
      </router-view>
    </main>

    <footer class="layout-footer">
      <AppFooter />
    </footer>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/layout' as *;
</style>
