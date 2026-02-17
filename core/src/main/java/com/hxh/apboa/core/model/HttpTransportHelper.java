package com.hxh.apboa.core.model;

import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.JdkHttpTransport;

import java.time.Duration;

/**
 * 描述：HttpTransport 生成器
 *
 * @author huxuehao
 **/
public class HttpTransportHelper {
    public static HttpTransport create() {
        HttpTransportConfig httpTransportConfig = HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .ignoreSsl(true)
                .build();

        return JdkHttpTransport.builder()
                .config(httpTransportConfig)
                .build();
    }
}
