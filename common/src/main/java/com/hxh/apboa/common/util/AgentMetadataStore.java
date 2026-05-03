package com.hxh.apboa.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 元数据存储，用于跨模块共享 Agent 的元数据（如 threadId）
 *
 * @author huxuehao
 **/
public final class AgentMetadataStore {
    public static final Map<String, Map<String, Object>> STORE = new ConcurrentHashMap<>();

    private AgentMetadataStore() {}

    /**
     * 设置 Agent 元数据
     *
     * @param agentId Agent ID
     * @param key     元数据键
     * @param value   元数据值
     */
    public static void put(String agentId, String key, Object value) {
        STORE.computeIfAbsent(agentId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    /**
     * 获取 Agent 元数据
     *
     * @param agentId Agent ID
     * @param key     元数据键
     * @param <T>     值类型
     * @return 元数据值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String agentId, String key) {
        Map<String, Object> meta = STORE.get(agentId);
        return meta == null ? null : (T) meta.get(key);
    }

    /**
     * 获取 Agent 全部元数据
     *
     * @param agentId Agent ID
     * @return 元数据 Map
     */
    public static Map<String, Object> getAll(String agentId) {
        return STORE.getOrDefault(agentId, Map.of());
    }

    /**
     * 移除 Agent 元数据
     *
     * @param agentId Agent ID
     */
    public static void remove(String agentId) {
        STORE.remove(agentId);
    }
}
