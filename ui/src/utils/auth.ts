import setting from "@/config/setting";
import cache from "@/utils/cache";
import Cookies from 'js-cookie';
import {type AccountVO} from "@/types";

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
  return cache.local.get(TokenKey) || '';
}

/**
 * 设置token
 * @param token token
 */
function setToken(token: string) {
  Cookies.set(ACCESS_TOKEN_COOKIE_KEY, token, {
    secure: setting.cookieSecure,
    sameSite: setting.cookieSameSite
  })
  cache.local.set(TokenKey, token);
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
  return cache.local.get(refreshTokenKey) || '';
}

/**
 * 设置refreshToken
 * @param refreshToken
 */
function setRefreshToken(refreshToken: string) {
  Cookies.set(REFRESH_TOKEN_COOKIE_KEY, refreshToken, {
    secure: setting.cookieSecure,
    sameSite: setting.cookieSameSite
  })
  cache.local.set(refreshTokenKey, refreshToken);
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

