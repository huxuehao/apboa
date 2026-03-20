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
                .config(getHttpTransportConfig())
                .build();
    }

    public static HttpTransport createOkHttpTransport() {
        return OkHttpTransport.builder()
                .config(getHttpTransportConfig())
                .build();
    }

    private static HttpTransportConfig getHttpTransportConfig() {
        return HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .ignoreSsl(true)
                .build();
    }
}
