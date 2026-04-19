/**
 * Doc 子应用入口
 *
 * @author huxuehao
 */

import 'ant-design-vue/dist/reset.css';
import 'highlight.js/styles/github.min.css';
import 'katex/dist/katex.min.css';
import '@/styles/markdown.scss';
import '@/assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import Antd from 'ant-design-vue';

import * as dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

import App from './App.vue';
import router from './router';

const app = createApp(App);

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);
app.use(pinia);
app.use(router);
app.use(Antd);

app.mount('#doc-app');
