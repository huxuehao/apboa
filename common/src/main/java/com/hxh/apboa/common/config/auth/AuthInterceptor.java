package com.hxh.apboa.common.config.auth;

import com.hxh.apboa.common.UserDetail;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.exception.NotAuthException;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.util.RedisUtils;
import com.hxh.apboa.common.util.TokenUtils;
import com.hxh.apboa.common.util.UserUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：权限认证拦截器
 *
 * @author huxuehao
 **/
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final int UNAUTHORIZED_STATUS = HttpServletResponse.SC_UNAUTHORIZED;
    private static final Map<Long, Role> ROLE_MAP = new ConcurrentHashMap<>();

    private final RedisUtils redisUtils;

    public AuthInterceptor(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    public static void setUserRole(Long userId, Role role) {
        ROLE_MAP.put(userId, role);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查是否需要跳过认证
        if (shouldSkipAuthentication(handlerMethod)) {
            log.debug("跳过认证: {}", handlerMethod.getMethod().getName());
            return true;
        }

        try {
            // 提取和验证Token
            String token = TokenUtils.getToken();
            Claims claims = TokenUtils.parseAndValidateToken(token);
            if (redisUtils.get(SysConst.LOGIN_USER_KEY + token) == null) {
                throw new NotAuthException("用户未登录");
            }

            // 将用户信息存储到请求中供后续使用
            request.setAttribute(SysConst.USER_DETAIL, JsonUtils.parse(claims.getSubject(), UserDetail.class));

            // 检查接口权限
            if (!checkRoleNeed(handlerMethod)) {
                handleRoleNeedFailure(response, "无权进行此操作");
                return false;
            }

            return true;
        } catch (Exception e) {
            handleAuthenticationFailure(response, e.getMessage());
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
    }

    /**
     * 检查是否需要跳过认证
     */
    private boolean shouldSkipAuthentication(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        return method.isAnnotationPresent(PassAuth.class) ||
                controllerClass.isAnnotationPresent(PassAuth.class);
    }

    /**
     * 检查接口权限
     */
    private boolean checkRoleNeed(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();

        if (method.getAnnotation(RoleNeed.class) != null) {
            Role[] roles = method.getAnnotation(RoleNeed.class).value();
            return roles.length > 0 && List.of(roles).contains(ROLE_MAP.get(UserUtils.getId()));
        }

        return true;
    }

    /**
     * 处理认证失败
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) {
        response.setStatus(UNAUTHORIZED_STATUS);
        response.setContentType("application/json;charset=UTF-8");

        try {
            // 返回标准的错误响应
            String errorJson = String.format("{\"code\": %d, \"msg\": \"%s\"}",
                    UNAUTHORIZED_STATUS, message);
            response.getWriter().write(errorJson);
        } catch (Exception e) {
            log.error("写入错误响应失败", e);
        }
    }
    /**
     * 处理认证失败
     */
    private void handleRoleNeedFailure(HttpServletResponse response, String message) {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");

        try {
            // 返回标准的错误响应
            String errorJson = String.format("{\"code\": %d, \"msg\": \"%s\"}",
                    400, message);
            response.getWriter().write(errorJson);
        } catch (Exception e) {
            log.error("写入错误响应失败", e);
        }
    }
}
