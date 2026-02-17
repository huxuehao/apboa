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
import router from "@/router";
import { message as AMessage } from 'ant-design-vue'
import { refreshToken as refreshTokenApi } from "@/api/auth";

NProgress.configure({ easing: "ease", speed: 150, showSpinner: false });

const instance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
});

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
      router.push("/login").then(() => {});
      return Promise.reject(msg);
    } else {
      AMessage.error(msg).then(() => {})
      return Promise.reject(msg);
    }
  },
  async (error) => {
    const originalRequest = error.config;
    if (error.response.status === 401 && !originalRequest.headers._retry) {
      originalRequest.headers._retry = true;
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        try {
          const response = await refreshTokenApi(refreshToken);
          const newToken = response.data.data.accessToken;
          const newRefreshToken = response.data.data.refreshToken;
          setToken(newToken);
          setRefreshToken(newRefreshToken);
          originalRequest.headers[setting.tokenHeader] = `Bearer ${newToken}`;
          return instance(originalRequest);
        } catch (err) {
          removeToken();
          removeRefreshToken();
          window.location.href = "/#/login";
          return Promise.reject(err);
        }
      } else {
        removeToken()
        window.location.href = "/#/login";
        return Promise.reject(error);
      }
    }
    AMessage.error('未知错误').then(() => {})
    return Promise.reject(error);
  }
);

export default instance;
