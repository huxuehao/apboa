package com.hxh.apboa.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.config.SpringContextHolder;

import java.time.Duration;

/**
 * 描述：json工具类
 *
 * @author huxuehao
 **/
public class JsonUtils {
    private static final ObjectMapper mapper;

    static {
        mapper = SpringContextHolder.getObjectMapper();
    }

    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof JsonNode node) {
            if (node.isNull()) {
                return null;
            }
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
    public static JsonNode valueToTree(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.valueToTree(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    public static JsonNode toJsonNode(Object obj) {
        try {
            return mapper.valueToTree(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JsonNode", e);
        }
    }

    public static JsonNode parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static <T> T parse(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static String getStringValue(JsonNode node, String fieldName, String defaultValue) {
        String stringValue = getStringValue(node, fieldName, false);
        return stringValue == null ? defaultValue : stringValue;
    }

    public static String getStringValue(JsonNode node, String fieldName, boolean required) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            if (required) {
                throw new IllegalArgumentException("Field " + fieldName + " is required but not found or null");
            }
            return null;
        }

        String value = node.get(fieldName).asText();
        if (required && (value == null || value.trim().isEmpty())) {
            throw new IllegalArgumentException("Field " + fieldName + " cannot be empty");
        }

        return value != null ? value.trim() : null;
    }

    public static int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isTextual()) {
            try {
                return Integer.parseInt(valueNode.asText());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    public static float getFloatValue(JsonNode node, String fieldName, float defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isNumber()) {
            return (float) valueNode.asDouble();
        } else if (valueNode.isTextual()) {
            try {
                return Float.parseFloat(valueNode.asText());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    public static double getDoubleValue(JsonNode node, String fieldName, double defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isNumber()) {
            return valueNode.asDouble();
        } else if (valueNode.isTextual()) {
            try {
                return Double.parseDouble(valueNode.asText());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    public static long getLongValue(JsonNode node, String fieldName, long defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isNumber()) {
            return valueNode.asLong();
        } else if (valueNode.isTextual()) {
            try {
                return Long.parseLong(valueNode.asText());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    public static boolean getBooleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else if (valueNode.isTextual()) {
            return Boolean.parseBoolean(valueNode.asText());
        } else if (valueNode.isNumber()) {
            return valueNode.asInt() != 0;
        }

        return defaultValue;
    }

    public static Duration getDurationValue(JsonNode node, String fieldName, Duration defaultValue) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        JsonNode valueNode = node.get(fieldName);
        try {
            if (valueNode.isNumber()) {
                return Duration.ofSeconds(valueNode.asLong());
            } else if (valueNode.isTextual()) {
                String durationStr = valueNode.asText().toLowerCase();
                long l = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                if (durationStr.endsWith("s")) {
                    return Duration.ofSeconds(l);
                } else if (durationStr.endsWith("m")) {
                    return Duration.ofMinutes(l);
                } else if (durationStr.endsWith("h")) {
                    return Duration.ofHours(l);
                } else {
                    return Duration.ofSeconds(Long.parseLong(durationStr));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse duration from field " + fieldName + ":" + valueNode.asText());
        }

        return defaultValue;
    }
}
