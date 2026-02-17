package com.hxh.apboa.core.tool;

import com.hxh.apboa.common.wrapper.ToolInfoWrapper;
import com.hxh.apboa.tool.service.ToolService;
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
public class ToolsSyncToDatabase implements ApplicationRunner {
    private final ToolService toolService;
    @Override
    public void run(ApplicationArguments args) {
        log.info("IAgentTool sync to DB starting");
        ArrayList<ToolInfoWrapper> toolInfoWrappers = new ArrayList<>();
        ToolsRegister.getTools().forEach(toolInfo -> {
            toolInfoWrappers.add(toolInfo.parseToolInfo());
        });

        toolService.SyncConfigToDatabase(toolInfoWrappers);
        log.info("IAgentTool sync to DB completed");
    }
}
