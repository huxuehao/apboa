package com.hxh.apboa.a2a.config;

import com.hxh.apboa.common.KvMap;
import com.hxh.apboa.common.util.FuncUtils;
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
            String value = kvMap.isEvn() ? System.getenv(kvMap.getValue()) : kvMap.getValue();
            if (!FuncUtils.isEmpty(value)) {
                authHeadersMap.put(kvMap.getKey(), value);
            }
        });

        return authHeadersMap;
    }
}
