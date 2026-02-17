<script setup lang="ts">
/**
 * 认证页面容器组件
 *
 * @author huxuehao
 */
import BackButton from './BackButton.vue'

interface Props {
  showBack?: boolean
  backTo?: string
}

withDefaults(defineProps<Props>(), {
  showBack: false,
  backTo: '/login',
})
</script>

<template>
  <div class="auth-container flex flex-center">
    <BackButton v-if="showBack" :to="backTo" />
    <ACard class="auth-card" :bordered="false" style="box-shadow: none">
      <slot />
    </ACard>
  </div>
  <div class="powered-by">Powered by Huxuehao</div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/auth' as *;
</style>
