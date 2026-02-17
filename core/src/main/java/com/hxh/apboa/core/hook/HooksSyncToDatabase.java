package com.hxh.apboa.core.hook;

import com.hxh.apboa.common.wrapper.HookConfigWrapper;
import com.hxh.apboa.hook.service.HookConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 描述：钩子加载到数据库
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class HooksSyncToDatabase implements ApplicationRunner {
    private final HookConfigService hookConfigService;
    @Override
    public void run(ApplicationArguments args) {
        log.info("IAgentHooks sync to DB starting");
        ArrayList<HookConfigWrapper> hookConfigWrappers = new ArrayList<>();
        HooksRegister.getHooks().forEach(hook -> {
            hookConfigWrappers.add(HookConfigWrapper.builder()
                    .name(((IAgentHook)hook).getName())
                    .description(((IAgentHook)hook).getDescription())
                    .classPath(hook.getClass().getName())
                    .build());
        });

        hookConfigService.SyncConfigToDatabase(hookConfigWrappers);
        log.info("IAgentHooks sync to DB completed");
    }
}
