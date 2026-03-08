import 'ant-design-vue/dist/reset.css';
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue';
import { message } from 'ant-design-vue'

import App from './App.vue'
import router from './router'
import { useAccountStore } from '@/stores'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)
app.use(router)
app.use(Antd)
message.config({
  top: '50px',
})


// 自定义权限指令
app.directive('permission', {
  async beforeMount(el, binding, vnode) {
    const requiredRoles = binding.value;
    if (!requiredRoles || requiredRoles.length === 0) {
      return;
    }

    const accountStore = useAccountStore()
    const hasPermission = accountStore.roles.some(role => requiredRoles.includes(role));
    if (!hasPermission) {
      el.style.display = 'none';
      el.style.visibility = 'hidden';
      el.style.height = '0';
      el.style.width = '0';
      el.style.overflow = 'hidden';
    }
  }
});

app.mount('#app')
