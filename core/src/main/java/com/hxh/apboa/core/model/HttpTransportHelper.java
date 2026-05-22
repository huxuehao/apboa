package com.hxh.apboa.core.model;

import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.JdkHttpTransport;
import io.agentscope.core.model.transport.OkHttpTransport;

import java.time.Duration;

/**
 * 描述：HttpTransport 生成器
 *
 * @author huxuehao
 **/
public class HttpTransportHelper {
    public static HttpTransport createJdkHttpTransport() {
        return JdkHttpTransport.builder()
                .config(getHttpTransportConfig(10, 60))
                .build();
    }

    public static HttpTransport createOkHttpTransport() {
        return createOkHttpTransport(10, 60);
    }

    public static HttpTransport createOkHttpTransport(int connectTimeout, int readTimeout) {
        return OkHttpTransport.builder()
                .config(getHttpTransportConfig(connectTimeout, readTimeout))
                .build();
    }

    private static HttpTransportConfig getHttpTransportConfig(int connectTimeout, int readTimeout) {
        return HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .readTimeout(Duration.ofSeconds(readTimeout))
                .ignoreSsl(true)
                .build();
    }
}
