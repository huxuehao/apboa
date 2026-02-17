<script setup lang="ts">
/**
 * 圆形返回按钮组件
 *
 * @author huxuehao
 */
import { useRouter } from 'vue-router'
import { LeftOutlined } from '@ant-design/icons-vue'

interface Props {
  to?: string
}

const props = withDefaults(defineProps<Props>(), {
  to: '/login',
})

const router = useRouter()

const handleBack = () => {
  router.push(props.to)
}
</script>

<template>
  <AButton
    class="auth-back-button"
    shape="circle"
    @click="handleBack"
  >
    <LeftOutlined />
  </AButton>
</template>

<style scoped lang="scss">
@use '@/styles/modules/auth' as *;
</style>
