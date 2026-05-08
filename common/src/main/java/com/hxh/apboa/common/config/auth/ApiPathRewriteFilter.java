package com.hxh.apboa.common.config.auth;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPathRewriteFilter implements Filter {

    // 配置需要去除前缀的路径模式
    private static final List<String> PATH_PATTERNS = Arrays.asList(
            "/api/",
            "/web/api/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // 检查是否需要去除前缀
        String matchedPattern = null;
        for (String pattern : PATH_PATTERNS) {
            if (requestUri.startsWith(contextPath + pattern)) {
                matchedPattern = pattern;
                break;
            }
        }

        if (matchedPattern != null) {
            HttpServletRequestWrapper wrapper = getHttpServletRequestWrapper(
                    requestUri, contextPath, httpRequest, matchedPattern
            );
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @NotNull
    private static HttpServletRequestWrapper getHttpServletRequestWrapper(
            String requestUri,
            String contextPath,
            HttpServletRequest httpRequest,
            String prefixToRemove) {

        // 替换路径前缀
        String finalNewUri = requestUri.replaceFirst("^" + contextPath + prefixToRemove, contextPath + "/");
        // 清理多余斜杠
        final String cleanedUri = finalNewUri.replaceAll("/+", "/");

        return new HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getRequestURI() {
                return cleanedUri;
            }

            @Override
            public String getServletPath() {
                String servletPath = cleanedUri.substring(contextPath.length());
                return servletPath.isEmpty() ? "/" : servletPath;
            }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = new StringBuffer();
                String scheme = httpRequest.getScheme();
                int port = httpRequest.getServerPort();

                url.append(scheme).append("://").append(httpRequest.getServerName());
                if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
                    url.append(":").append(port);
                }
                url.append(contextPath).append(getRequestURI());

                // 保留查询参数
                String queryString = httpRequest.getQueryString();
                if (queryString != null) {
                    url.append("?").append(queryString);
                }
                return url;
            }
        };
    }
}
