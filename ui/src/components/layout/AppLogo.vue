<script setup lang="ts">
/**
 * 应用Logo组件
 *
 * @author huxuehao
 */
interface Props {
  projectName?: string
}

withDefaults(defineProps<Props>(), {
  projectName: 'APBOA',
})
</script>

<template>
  <div class="app-logo flex items-center gap-sm pl-md cursor-pointer">
    <div class="logo-icon flex items-center justify-center">
      <img src="@/assets/images/logo/logo.png" alt="logo" width="32">
    </div>
    <span class="project-name">{{ projectName }}</span>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/components/logo' as *;
</style>
