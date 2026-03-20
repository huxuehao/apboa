package com.hxh.apboa.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestHolder {
    // 使用InheritableThreadLocal支持子线程继承
    private static final InheritableThreadLocal<HttpServletRequest> REQUEST_HOLDER = new InheritableThreadLocal<>();

    public static void setRequest(HttpServletRequest request) {
        REQUEST_HOLDER.set(request);
    }

    public static HttpServletRequest getRequest() {
        HttpServletRequest request = REQUEST_HOLDER.get();
        if (request != null) {
            return request;
        }

        // 降级方案：尝试从RequestContextHolder获取
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
            // 缓存到ThreadLocal，避免重复获取
            REQUEST_HOLDER.set(request);
        }
        return request;
    }

    public static void clear() {
        REQUEST_HOLDER.remove();
    }
}
