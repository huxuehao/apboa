package com.hxh.apboa.common.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

/**
 * 描述：拦截器配置
 *
 * @author huxuehao
 **/
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(RedisUtils.class)
public class InterceptorConfig implements WebMvcConfigurer {
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter =
                new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(0, jackson2HttpMessageConverter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /* 拦截所有的请求，通过请求映射到的方法上的注解进行判断是否需要权限验证 */
        registry.addInterceptor(new AuthInterceptor(redisUtils))
                .addPathPatterns("/**")
                .excludePathPatterns("/web/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /* 静态资源处理器，将/apboa/**请求指引到classpath:/META-INF/resources/apboa/下 */
        registry.addResourceHandler("/web/**").addResourceLocations("classpath:/META-INF/resources/web/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/META-INF/resources/web/images/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 添加视图控制器，引导静态请求
        registry.addViewController("/web/doc/").setViewName("forward:/web/doc/index.html");
        registry.addViewController("/web/doc").setViewName("forward:/web/doc/index.html");
        registry.addViewController("/web/").setViewName("forward:/web/index.html");
        registry.addViewController("/web").setViewName("forward:/web/index.html");
        registry.addViewController("/").setViewName("forward:/web/index.html");
        registry.addViewController("/doc.html").setViewName("forward:/web/doc/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        /* 添加映射路径 */
        registry.addMapping("/**")
                /* 设置放行哪些原始域 SpringBoot2.4.4下低版本使用.allowedOrigins("*") */
                .allowedOrigins("*")
                /* 放行哪些请求方式 */
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                // .allowedMethods("*") //或者放行全部
                /* 放行哪些原始请求头部信息 */
                .allowedHeaders("*");
    }
}
