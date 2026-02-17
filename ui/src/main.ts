import 'ant-design-vue/dist/reset.css';
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue';
import { message } from 'ant-design-vue'

import App from './App.vue'
import router from './router'
import { useAccountStore } from '@/stores'
import { useWebSocket } from '@/composables/useWebSocket';

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
message.config({
  top: '50px',
})


// 自定义权限指令
app.directive('permission', {
  async mounted(el, binding) {
    const requiredRoles = binding.value;
    if (!requiredRoles || requiredRoles.length === 0) {
      return;
    }

    const accountStore = useAccountStore()
    const hasPermission = accountStore.roles.some(role => requiredRoles.includes(role));
    if (!hasPermission) {
      el.parentNode?.removeChild(el);
    }
  }
});

// 尝试初始化ws
const { initWS } = useWebSocket();
initWS()

app.mount('#app')
