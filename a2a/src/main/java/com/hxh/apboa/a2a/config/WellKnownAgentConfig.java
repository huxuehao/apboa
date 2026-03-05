package com.hxh.apboa.a2a.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述：WellKnownAgentCardConfig
 *
 * @author huxuehao
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WellKnownAgentConfig {
    private String agentName;
    private String baseUrl;
    private String relativeCardPath;
    private List<KvMap> authHeaders;

    public Map<String, String> getRealAuthHeaders() {
        Map<String, String> authHeadersMap = new HashMap<>();

        authHeaders.forEach(kvMap -> {
            if (kvMap.isEvn()) {
                authHeadersMap.put(kvMap.getKey(), System.getenv(kvMap.getKey()));
            } else {
                authHeadersMap.put(kvMap.getKey(), kvMap.getValue());
            }
        });

        return authHeadersMap;
    }
}
