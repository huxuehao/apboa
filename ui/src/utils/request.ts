import axios from "axios";
import { tansRequestParams, checkRequestRepeat } from "@/utils/tools";
import {
  getToken,
  getRefreshToken,
  setToken,
  setRefreshToken,
  removeToken,
  removeRefreshToken
} from "./auth";
import setting from "@/config/setting";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { message as AMessage } from 'ant-design-vue'
import { refreshToken as refreshTokenApi } from "@/api/auth";

type TokenRefreshCallback = (newToken: string) => void;

NProgress.configure({ easing: "ease", speed: 150, showSpinner: false });

const instance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
});

// 用于标记是否正在刷新令牌，防止并发请求重复刷新
let isRefreshing = false;
// 存储等待刷新令牌完成后重试的请求
let requestsQueue:TokenRefreshCallback[] = [];

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    NProgress.start();

    // 携带凭证
    const needToken = !((config.headers || {}).token === false);
    const token = getToken();
    if (needToken && token) {
      config.headers[setting.tokenHeader] = 'Bearer ' + token;
    }

    // 参数转换
    if (config.params) {
      // 请求参数转换
      let url = config.url + "?" + tansRequestParams(config.params);
      url = url.slice(0, -1);
      config.params = {};
      config.url = url;
    }

    // 检测是否重复操作
    if (
      (config.headers || {}).unrepeat &&
      checkRequestRepeat(<string>config.url, {
        ...config.data,
        ...config.params,
      })
    ) {
      AMessage.warning('请勿重复操作').then(() => {})
      NProgress.done();
      return Promise.reject("请勿重复操作");
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    NProgress.done();
    if (
      response.request.responseType === "blob" ||
      response.request.responseType === "arraybuffer"
    ) {
      return response;
    }

    const code = response.data.code || 500;
    const msg = response.data.msg || "未知异常";
    if(code == 200) {
      return response;
    } else if (code == 202) {
      AMessage.info(msg).then(() => {})
      return response;
    } else if(code === 401) {
      AMessage.warning('登录状态已失效，请重新登录').then(() => {})
      window.location.href = '/#/login';
      window.location.reload();
      return Promise.reject(msg);
    } else {
      AMessage.error(msg).then(() => {})
      return Promise.reject(msg);
    }
  },
  async (error) => {
    const originalRequest = error.config;
    // 1. 非401错误直接抛出
    if (!error.response || error.response.status !== 401) {
      NProgress.done();
      AMessage.error(error.message || '未知错误').then(() => {})
      return Promise.reject(error);
    }

    // 2. 如果是刷新令牌接口本身返回401，直接清理令牌并跳转登录
    if (originalRequest.headers[setting.refreshTokenRequest] || originalRequest.url.includes('/api/auth/refresh-token')) {
      removeToken();
      removeRefreshToken();
      requestsQueue = [];
      AMessage.warning('刷新令牌失败，请重新登录').then(() => {})
      window.location.href = '/#/login';
      window.location.reload();
      return Promise.reject('刷新令牌失败');
    }

    // 3. 防止重复重试
    if (originalRequest.headers._retry) {
      removeToken();
      removeRefreshToken();
      requestsQueue = [];
      AMessage.warning('登录状态已失效，请重新登录').then(() => {})
      window.location.href = '/#/login';
      window.location.reload();
      return Promise.reject('令牌重试失败');
    }

    originalRequest.headers._retry = true;

    // 4. 处理令牌刷新逻辑
    if (!isRefreshing) {
      isRefreshing = true;
      const refreshToken = getRefreshToken();

      if (!refreshToken) {
        removeToken();
        removeRefreshToken();
        requestsQueue = [];
        AMessage.warning('无刷新令牌，请重新登录').then(() => {})
        window.location.href = '/#/login';
        window.location.reload();
        isRefreshing = false;
        return Promise.reject('无刷新令牌');
      }

      try {
        // 标记刷新令牌请求，便于识别
        const refreshResponse = await refreshTokenApi(refreshToken);

        const newToken = refreshResponse.data.data.accessToken;
        const newRefreshToken = refreshResponse.data.data.refreshToken;

        setToken(newToken);
        setRefreshToken(newRefreshToken);

        // 更新原请求的令牌
        originalRequest.headers[setting.tokenHeader] = `Bearer ${newToken}`;

        // 重试队列中所有等待的请求
        requestsQueue.forEach(cb => cb(newToken));
        requestsQueue = [];

        isRefreshing = false;
        return instance(originalRequest);
      } catch (err) {
        // 刷新令牌失败，清理所有状态
        removeToken();
        removeRefreshToken();
        requestsQueue = [];
        isRefreshing = false;

        AMessage.warning('登录已过期，请重新登录').then(() => {})
        window.location.href = '/#/login';
        window.location.reload();
        return Promise.reject(err);
      }
    } else {
      // 有其他请求正在刷新令牌，加入队列等待
      return new Promise((resolve) => {
        requestsQueue.push((newToken: string) => {
          originalRequest.headers[setting.tokenHeader] = `Bearer ${newToken}`;
          resolve(instance(originalRequest));
        });
      });
    }
  }
);

export default instance;
