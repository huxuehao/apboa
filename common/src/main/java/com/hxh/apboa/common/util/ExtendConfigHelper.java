package com.hxh.apboa.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 扩展配置解析工具
 * 用于从 extendConfig (JsonNode) 中解析 headers、queryParams、bodyParams
 *
 * @author huxuehao
 */
public final class ExtendConfigHelper {

    private ExtendConfigHelper() {
    }

    /**
     * 从 JsonNode 解析并填充到 configWrapper
     * 仅在 configWrapper 中对应字段为空时填充
     */
    public static void fillIfAbsent(ModelConfigWrapper configWrapper, JsonNode extendConfig) {
        if (extendConfig == null || extendConfig.isNull() || !extendConfig.isObject()) {
            return;
        }
        if (configWrapper.getHeaders() == null && extendConfig.has("headers")) {
            configWrapper.setHeaders(parseStringMap(extendConfig.get("headers")));
        }
        if (configWrapper.getQueryParams() == null && extendConfig.has("queryParams")) {
            configWrapper.setQueryParams(parseStringMap(extendConfig.get("queryParams")));
        }
        if (configWrapper.getBodyParams() == null && extendConfig.has("bodyParams")) {
            configWrapper.setBodyParams(parseObjectMap(extendConfig.get("bodyParams")));
        }
        if (configWrapper.getFixedSystemMessage() == null && extendConfig.has("fixedSystemMessage")) {
            configWrapper.setFixedSystemMessage(parseBoolean(extendConfig.get("fixedSystemMessage")));
        }
    }

    /**
     * 强制填充（覆盖已有值），用于 agent 级 modelParamsOverride
     */
    public static void fillOverride(ModelConfigWrapper configWrapper, JsonNode extendConfig) {
        if (extendConfig == null || extendConfig.isNull() || !extendConfig.isObject()) {
            return;
        }
        if (extendConfig.has("headers")) {
            configWrapper.setHeaders(parseStringMap(extendConfig.get("headers")));
        }
        if (extendConfig.has("queryParams")) {
            configWrapper.setQueryParams(parseStringMap(extendConfig.get("queryParams")));
        }
        if (extendConfig.has("bodyParams")) {
            configWrapper.setBodyParams(parseObjectMap(extendConfig.get("bodyParams")));
        }
        if (extendConfig.has("fixedSystemMessage")) {
            configWrapper.setFixedSystemMessage(parseBoolean(extendConfig.get("fixedSystemMessage")));
        }
    }

    public static Map<String, String> parseStringMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            JsonNode valueNode = node.get(key);
            if (valueNode != null && !valueNode.isNull()) {
                map.put(key, valueNode.asText());
            }
        }
        return map.isEmpty() ? Map.of() : map;
    }

    public static boolean parseBoolean(JsonNode node) {
        if (node == null) {
            return false;
        }

        return node.isBoolean() && node.asBoolean(false);
    }

    public static Map<String, Object> parseObjectMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, Object> map = new HashMap<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            JsonNode valueNode = node.get(key);
            if (valueNode != null && !valueNode.isNull()) {
                map.put(key, jsonNodeToObject(valueNode));
            }
        }
        return map.isEmpty() ? Map.of() : map;
    }

    private static Object jsonNodeToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            try {
                // 尝试将字符串转对象
                return JsonUtils.parse(node.asText());
            } catch (Exception e) {
                return node.asText();
            }

        }
        if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            }
            if (node.isLong()) {
                return node.asLong();
            }
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            return JsonUtils.parse(node.toString(), java.util.List.class);
        }
        if (node.isObject()) {
            return JsonUtils.parse(node.toString(), Map.class);
        }
        return node.asText();
    }
}
