package com.hxh.apboa.common.util;

import com.hxh.apboa.common.config.SpringContextHolder;
import com.hxh.apboa.common.consts.SysConst;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * 描述：token工具类
 *
 * @author huxuehao
 **/
public class TokenUtils {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN = "accessToken";
    private static final String COOKIE_AUTH = "apboa-access-token";

    /**
     * 创建token
     * @param id        ID
     * @param subject   主题
     * @param ttlMillis 过期时间
     */
    public static String createToken(String id, Object subject, long ttlMillis) {
        String secret = SpringContextHolder.getProperty(SysConst.JWT_SECRET_KEY);
        return createToken(id, JsonUtils.toJsonStr(subject), ttlMillis, secret);
    }

    /**
     * 创建token
     * @param id        ID
     * @param subject   主题
     * @param ttlMillis 过期时间
     */
    public static String createToken(String id, String subject, long ttlMillis, String secret) {
        /* token生成的时间 */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        /* 创建JWT构建器 */
        JwtBuilder builder = Jwts.builder()
                .claims()
                .empty()
                .add(SysConst.LOGIN_USER_KEY, UUID.randomUUID().toString())
                .and()
                .id(id)
                .issuedAt(now)
                .subject(subject)
                .signWith(generalKey(secret));

        /* 设置过期时间 */
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.expiration(exp);
        }

        /* 返回token */
        return builder.compact();
    }

    /**
     * 解密token
     * @param token token
     */
    public static Claims parseToken(String token) {
        String secret = SpringContextHolder.getProperty(SysConst.JWT_SECRET_KEY);
        return parseToken(token, secret);
    }

    /**
     * 解密token
     * @param token token
     */
    public static Claims parseToken(String token, String secret) {
        JwtParser parser = Jwts.parser()
                .verifyWith(generalKey(secret))
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * 由字符串生成加密key
     *
     * @param secret 密钥
     * @return 秘钥
     */
    public static SecretKey generalKey(String secret){
        /* 本地配置文件中加密的密文 */
        byte[] encodedKey = Base64.getDecoder().decode(secret);
        /* 根据给定的字节数组使用HMAC-SHA加密算法构造一个密钥。*/
        return Keys.hmacShaKeyFor(encodedKey);
    }

    public static String getToken() {
        HttpServletRequest request = WebUtils.getRequest();
        if (request == null) {
            throw new RuntimeException("HttpServletRequest 为空, 获取Token 失败");
        }
        return extractTokenFromRequest(request);
    }

    /**
     * 从请求中提取Token
     */
    private static String extractTokenFromRequest(HttpServletRequest request) {
        // 优先从Header获取
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!FuncUtils.isEmpty(authHeader)) {
            return extractBearerToken(authHeader);
        }

        // 从参数获取
        String paramToken = request.getParameter(TOKEN);
        if (!FuncUtils.isEmpty(paramToken)) {
            return paramToken;
        }

        // 从 cookie 中获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            Optional<Cookie> first = Arrays.stream(cookies).filter((cookie) -> Objects.equals(cookie.getName(), COOKIE_AUTH)).findFirst();
            if (first.isPresent()) {
                return first.get().getValue();
            }
        }

        throw new RuntimeException("未提供认证Token");
    }

    /**
     * 提取并验证Bearer Token格式
     */
    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException("Token格式错误，必须使用Bearer认证方案");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (FuncUtils.isEmpty(token)) {
            throw new RuntimeException("Token不能为空");
        }

        return token;
    }

    /**
     * 解析并验证Token
     */
    public static Claims parseAndValidateToken(String token) {
        try {
            Claims claims = TokenUtils.parseToken(token);

            // 额外的Token验证
            validateTokenClaims(claims);

            return claims;

        } catch (JwtException e) {
            throw new RuntimeException("Token无效或已过期");
        } catch (Exception e) {
            throw new RuntimeException("认证系统错误");
        }
    }

    /**
     * 额外的Token声明验证
     */
    private static void validateTokenClaims(Claims claims) {
        // 验证Token是否在有效期内
        if (claims.getExpiration() == null) {
            throw new RuntimeException("Token缺少过期时间");
        }

        // 验证必要字段
        if (FuncUtils.isEmpty(claims.getId())) {
            throw new RuntimeException("Token缺少用户标识");
        }
    }
}
