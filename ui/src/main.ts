import 'ant-design-vue/dist/reset.css';
import 'highlight.js/styles/github.min.css';
import 'katex/dist/katex.min.css';
import './styles/markdown.scss';
import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import Antd from 'ant-design-vue';
import { message } from 'ant-design-vue'

// dayjs 并设置为中文
import * as dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

import App from './App.vue'
import router from './router'
import { useAccountStore } from '@/stores'
import ApboaModal from '@/components/common/ApboaModal.vue'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)
app.use(router)
app.use(Antd)
app.component('ApboaModal', ApboaModal)
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
