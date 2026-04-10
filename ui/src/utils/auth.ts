import setting from "@/config/setting";
import cache from "@/utils/cache";
import Cookies from 'js-cookie';
import {type AccountVO, type TokenType} from "@/types";

const TokenKey = setting.systemName + '-' + setting.accessToken;
const refreshTokenKey = setting.systemName + '-' + setting.refreshToken;
const userKey = setting.systemName + '-' + setting.user;
const ACCESS_TOKEN_COOKIE_KEY = 'apboa-access-token';
const REFRESH_TOKEN_COOKIE_KEY = 'apboa-refresh-token';

/**
 * 获取token
 * @returns token
 */
function getToken(): string {
  const token = cache.local.getJSON(TokenKey, { value: null, ttl: 0 })
  if(token.value) {
    if(token.ttl === '-1') {
      return token.value
    }
    const currentTime = new Date().getTime()
    if(currentTime < Number(token.ttl)) {
      return token.value
    }
  }
  return '';
}

/**
 * 设置token
 * @param token token
 */
function setToken(token: TokenType) {
  Cookies.set(ACCESS_TOKEN_COOKIE_KEY, token.value, {
    secure: setting.cookieSecure,
    sameSite: setting.cookieSameSite,
    expires: 7, // 7天过期，关闭浏览器不消失
    path: '/'    // 全站有效
  })
  cache.local.setJSON(TokenKey, token);
}

/**
 * 清除token
 */
function removeToken() {
  Cookies.remove(ACCESS_TOKEN_COOKIE_KEY)
  cache.local.remove(TokenKey);
}

/**
 * 获取refreshToken
 * @returns token
 */
function getRefreshToken(): string {
  const refreshToken = cache.local.getJSON(refreshTokenKey, { value: null, ttl: 0 });
  if(refreshToken.value) {
    if(refreshToken.ttl === -1) {
      return refreshToken.value
    }
    const currentTime = new Date().getTime()
    if(currentTime < refreshToken.ttl) {
      return refreshToken.value
    }
  }
  return '';
}

/**
 * 设置refreshToken
 * @param refreshToken
 */
function setRefreshToken(refreshToken: TokenType) {
  Cookies.set(REFRESH_TOKEN_COOKIE_KEY, refreshToken.value, {
    secure: setting.cookieSecure,
    sameSite: setting.cookieSameSite,
    expires: 7, // 7天过期，关闭浏览器不消失
    path: '/'    // 全站有效
  })
  cache.local.setJSON(refreshTokenKey, refreshToken);
}

/**
 * 清除refreshToken
 */
function removeRefreshToken() {
  Cookies.remove(REFRESH_TOKEN_COOKIE_KEY)
  cache.local.remove(refreshTokenKey);
}

/**
 * 获取User
 */
function getUser():AccountVO {
  return cache.local.getJSON(userKey);
}

/**
 * 设置User
 */
function setUser(userInfo: AccountVO) {
  cache.local.setJSON(userKey, userInfo);
}

/**
 * 清除User
 */
function removeUser() {
  cache.local.remove(userKey);
}

export {
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
  removeRefreshToken,
  getUser,
  setUser,
  removeUser
};

