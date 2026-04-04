package com.hxh.apboa.common.config.auth;

import com.hxh.apboa.common.UserDetail;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.exception.NotAuthException;
import com.hxh.apboa.common.util.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：权限认证拦截器
 *
 * @author huxuehao
 **/
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private static final int UNAUTHORIZED_STATUS = HttpServletResponse.SC_UNAUTHORIZED;
    private static final Map<Long, Role> ROLE_MAP = new ConcurrentHashMap<>();
    /** 存储有效的SK ID集合，用于快速校验SK是否有效 */
    private static final Set<Long> SK_ID_SET = ConcurrentHashMap.newKeySet();

    private final RedisUtils redisUtils;

    public AuthInterceptor(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    public static void setUserRole(Long userId, Role role) {
        ROLE_MAP.put(userId, role);
    }

    /**
     * 添加有效的SK ID到集合中（创建SK时调用）
     *
     * @param skId SK的ID
     */
    public static void addSkId(Long skId) {
        SK_ID_SET.add(skId);
    }

    /**
     * 批量移除SK ID（批量删除SK时调用）
     *
     * @param skIds SK的ID列表
     */
    public static void removeSkIds(List<Long> skIds) {
        skIds.forEach(SK_ID_SET::remove);
    }

    /**
     * 检查SK ID是否有效
     *
     * @param skId SK的ID
     * @return 是否有效
     */
    public static boolean isSkIdValid(Long skId) {
        return SK_ID_SET.contains(skId);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        RequestHolder.setRequest(request);

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查是否需要跳过认证
        if (shouldSkipAuthentication(handlerMethod)) {
            log.debug("跳过认证: {}", handlerMethod.getMethod().getName());
            return true;
        }

        try {
            // 提取Token
            String token = TokenUtils.getToken();

            // SK token 走独立的认证分支
            if (token.startsWith("sk-")) {
                return handleSkToken(token, request, response, handlerMethod);
            }

            // 常规 JWT token 认证
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
        RequestHolder.clear();
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
     * 处理SK token认证：验证token有效性并检查接口是否允许SK访问
     *
     * @param token         以sk-开头的完整token
     * @param request       HTTP请求
     * @param response      HTTP响应
     * @param handlerMethod 处理方法
     * @return 是否通过认证
     */
    private boolean handleSkToken(String token,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  HandlerMethod handlerMethod) {
        // 检查接口是否被 @SkAccess 标记
        if (!checkSkAccess(handlerMethod)) {
            handleAuthenticationFailure(response, "该接口不支持SK访问");
            return false;
        }

        try {
            // 剔除 sk- 前缀，解压还原原始JWT后解析
            String jwtToken = TokenUtils.decompressJwt(token.substring(3));
            Claims claims = TokenUtils.parseAndValidateToken(jwtToken);

            // subject 即为创建SK时传入的name（JsonUtils.toJsonStr对String直接返回原值）
            String name = claims.getSubject();
            Long id = Long.parseLong(claims.getId());

            // 校验SK ID是否有效（是否被删除）
            if (!isSkIdValid(id)) {
                handleAuthenticationFailure(response, "SK已失效");
                return false;
            }

            // 构造仅含基本信息的UserDetail
            UserDetail userDetail = UserDetail.builder()
                    .id(id)
                    .name(name)
                    .username(name)
                    .build();

            request.setAttribute(SysConst.USER_DETAIL, userDetail);
            return true;
        } catch (Exception e) {
            handleAuthenticationFailure(response, e.getMessage());
            return false;
        }
    }

    /**
     * 检查方法或类是否被 @SkAccess 标记
     *
     * @param handlerMethod 处理方法
     * @return 是否允许SK访问
     */
    private boolean checkSkAccess(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();
        return method.isAnnotationPresent(SkAccess.class) ||
                controllerClass.isAnnotationPresent(SkAccess.class);
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
