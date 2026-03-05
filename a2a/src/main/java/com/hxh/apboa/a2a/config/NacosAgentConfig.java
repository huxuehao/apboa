package com.hxh.apboa.a2a.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Properties;

/**
 * 描述：
 *
 * @author huxuehao
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NacosAgentConfig {
    private String agentName;
    private List<KvMap> nacosProperties;

    public Properties getNacosProperties() {
        Properties properties = new Properties();

        nacosProperties.forEach(kvMap -> {
            if (kvMap.isEvn()) {
                properties.put(kvMap.getKey(), System.getenv(kvMap.getKey()));
            } else {
                properties.put(kvMap.getKey(), kvMap.getValue());
            }
        });

        return properties;
    }
}
