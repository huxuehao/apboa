<script lang="ts">
import { defineComponent, ref, computed, h } from 'vue'
import { useRoute } from 'vue-router'
import { useAccountStore } from '@/stores'
import { chatKeyToken } from '@/api/auth.ts'
import { getAgentIdByChatKey } from '@/api/agentChatKey.ts'
import Chat from '@/views/Chat/index.vue'

export default defineComponent({
  name: 'ChatWrapper',
  emits: ['error'],
  async setup(props, { emit }) {
    const chatAgentId = ref<string>()
    const route = useRoute()
    const chatKey = computed(() => (route.params.chatKey as string) || '')
    const accountStore = useAccountStore()
    accountStore.initStore()

    const isLoggedIn = accountStore.isLoggedIn
    try {
      if (!isLoggedIn) {
        const response = await chatKeyToken(chatKey.value)
        const data = response.data.data
        if (!data) {
          window.location.href = '/#/login';
          return
        }
        accountStore.setAccessInfo(data)
        const userDetail = data.userDetail
        accountStore.setUserInfo({
          id: userDetail.id,
          nickname: userDetail.name,
          email: userDetail.email,
          username: userDetail.account,
          enabled: true,
          roles: []
        })
      }

      const res = await getAgentIdByChatKey(chatKey.value)
      chatAgentId.value = res.data.data
    } catch (error) {
      // 使用 h 函数返回虚拟节点
      console.error('初始化失败:', error)
      emit('error', error)
    }

    // 使用 h 函数返回虚拟节点
    return () => h(Chat, {
      chatAgentId: chatAgentId.value,
      showAccount: false
    })
  }
})
</script>
