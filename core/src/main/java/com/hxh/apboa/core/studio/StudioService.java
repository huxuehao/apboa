package com.hxh.apboa.core.studio;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.StudioConfig;
import com.hxh.apboa.studio.service.AgentStudioService;
import com.hxh.apboa.studio.service.StudioConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 描述：StudioService
 *
 * @author huxuehao
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class StudioService {
    private final AgentStudioService agentStudioService;
    private final StudioConfigService studioConfigService;

    public boolean init(AgentDefinition definition) {
        Long studioIdByAgentId = agentStudioService.getStudioIdByAgentId(definition.getId());
        if (studioIdByAgentId != null) {
            StudioConfig studioConfig = studioConfigService.getById(studioIdByAgentId);
            if (studioConfig != null) {
                try {
                    StudioManagerUtils.initOnce(studioConfig.getUrl(), studioConfig.getProject(), definition.getAgentCode());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return false;
                }
                return true;
            }
        }

        return false;
    }
}
