package com.hxh.apboa.common.key;

/**
 * 描述：mcp sse 协议 key
 *
 * @author huxuehao
 **/
public record McpSseKey() {
    public static String name = "name";
    public static String url = "url";
    public static String headers = "headers";
    public static String queryParams = "queryParams";
    public static String timeout = "timeout";
    public static String pattern = "pattern";
}
