package com.hxh.apboa.core.tool;

import io.agentscope.core.model.ExecutionConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 描述：自定义 Toolkit 配置
 * <p>
 * 支持通过配置文件（application.yml）配置 Toolkit 的各项参数，
 * 包括并行执行、工具删除、执行超时等。
 *
 * @author wei.liu
 **/
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "apboa.tool")
public class CustomToolkitConfig {

    /**
     * 是否允许并行执行多个工具，默认 false
     */
    private boolean parallel = false;

    /**
     * 是否允许删除工具，默认 false
     */
    private boolean allowToolDeletion = false;

    /**
     * 工具执行超时时间（秒），默认 60 秒
     */
    private long executionTimeoutSeconds = 60;

    /**
     * 构建 ExecutionConfig
     */
    public ExecutionConfig toExecutionConfig() {
        return ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(executionTimeoutSeconds))
                .build();
    }
}
