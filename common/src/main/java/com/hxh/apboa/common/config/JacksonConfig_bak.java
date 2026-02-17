package com.hxh.apboa.common.config;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.module.SimpleModule;
//import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
///**
// * Jackson配置类
// * 用于解决前端JavaScript中Long类型精度丢失问题
// *
// * @author huxuehao
// */
//@Configuration
//public class JacksonConfig_bak {
//
//    /**
//     * 配置Jackson的ObjectMapper
//     * 将所有Long类型(包括Long和long)序列化为String类型
//     *
//     * @param builder Jackson2ObjectMapperBuilder
//     * @return 配置好的ObjectMapper
//     */
//    @Bean
//    @Primary
//    @ConditionalOnMissingBean(ObjectMapper.class)
//    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
//        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
//
//        JavaTimeModule javaTimeModule = new JavaTimeModule();
//        javaTimeModule.addSerializer(
//                LocalDateTime.class,
//                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
//        );
//
//        objectMapper.registerModule(javaTimeModule);
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//
//
//        // 创建自定义模块
//        SimpleModule simpleModule = new SimpleModule();
//
//        // 将Long和long类型序列化为String
//        // 避免前端JavaScript精度丢失
//        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
//        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
//
//        // 注册模块
//        objectMapper.registerModule(simpleModule);
//
//        // 设置为 false 后，Jackson 会忽略 “空 Bean” 的限制，直接将其序列化为空 JSON 对象（{}），不抛出异常。
//        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//
//        return objectMapper;
//    }
//}
