export default {
  systemName: "apboa",
  repeatReqInterval: 500, // 重复请求时间间隔（毫秒）
  tokenHeader: "Authorization",
  refreshTokenRequest: "Is_Refresh_Token_Request",
  accessToken: "accessToken",
  refreshToken: "refreshToken",
  user: "user",
  cookieSecure: false,
  cookieSameSite: 'lax' as "lax" | "strict" | "Strict" | "Lax" | "none" | "None",
  whiteList: [
    "/login"
  ],
};
