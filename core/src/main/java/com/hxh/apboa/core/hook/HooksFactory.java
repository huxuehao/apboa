package com.hxh.apboa.core.hook;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.HookConfig;
import com.hxh.apboa.hook.service.AgentHookService;
import com.hxh.apboa.hook.service.HookConfigService;
import io.agentscope.core.hook.Hook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：钩子工厂
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class HooksFactory {
    private final AgentHookService agentHookService;
    private final HookConfigService hookConfigService;

    public List<Hook> getHooks(AgentDefinition agentDefinition) {
        List<Long> hookIds = agentHookService.getHookIds(agentDefinition.getId());

        if (hookIds.isEmpty()) {
            return List.of();
        }

        List<Hook> hooks = new ArrayList<>();
        hookConfigService.listByIds(hookIds)
                .stream()
                .filter(HookConfig::getEnabled)
                .forEach(hookConfig -> {
                    hooks.add(HooksRegister.getHook(hookConfig.getClassPath()));
                });

        return hooks;
    }
}
