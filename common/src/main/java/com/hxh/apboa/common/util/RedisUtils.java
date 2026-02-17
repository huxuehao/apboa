package com.hxh.apboa.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 描述：Redis 工具类
 *
 * @author huxuehao
 **/
@Component
public class RedisUtils {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 设置缓存（无过期时间）
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(value));
    }

    /**
     * 设置缓存（无过期时间）
     * @param key 键
     * @param value 值
     */
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void setEx(String key, Object value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JsonUtils.toJsonStr(value), timeout, unit);
    }

    /**
     * 缓存字符串
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void setEx(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 字符串值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存并反序列化为指定对象
     * @param key 键
     * @param clazz 目标对象的类
     * @param <T> 泛型
     * @return 反序列化后的对象
     */
    public <T> T get(String key, Class<T> clazz) {
        String valueStr = get(key);
        if (valueStr == null) {
            return null;
        }
        return JsonUtils.parse(valueStr, clazz);
    }

    /**
     * 删除缓存
     * @param key 键
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 判断缓存是否存在
     * @param key 键
     */
    public boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }
}
