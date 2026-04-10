package com.hxh.apboa.core.endpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.config.auth.ChatKeyAccess;
import com.hxh.apboa.common.config.auth.SkAccess;
import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.enums.ToolType;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.tool.IAgentTool;
import com.hxh.apboa.core.tool.ToolsRegister;
import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;
import com.hxh.apboa.core.tool.dynamices.ToolInstanceLoadFactory;
import com.hxh.apboa.tool.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * 描述：
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/agent/endpoint")
@RequiredArgsConstructor
public class EndPoint {
    private final ToolService toolService;
    private final ToolReflectionInvoker toolReflectionInvoker;

    @SkAccess
    @ChatKeyAccess
    @PostMapping("/do/{toolName}/tool")
    public R<?> doTool(@PathVariable("toolName") String toolName , @RequestBody LinkedHashMap<String, Object> args) {
        ToolConfig toolConfig = toolService.getOne(new LambdaQueryWrapper<ToolConfig>().eq(ToolConfig::getToolId, toolName));

        if (toolConfig == null) {
            return R.data("工具调用失败");
        }

        if (toolConfig.getToolType() == ToolType.BUILTIN) {
            IAgentTool iTool = ToolsRegister.getTool(toolConfig.getClassPath());
            Object result = toolReflectionInvoker.invokeTool(iTool, toolName, args);

            return R.data(result);
        } else {
            // 拿到动态工具的实例
            IDynamicAgentTool dynamicAgentTool = ToolInstanceLoadFactory
                    .getInstanceLoader(toolConfig.getLanguage()).loadInstance(toolConfig.getCode());

            List<Object> args_ = new LinkedList<>();
            if (!args.isEmpty()) {
                args.forEach((key, value) -> {
                    args_.add(value);
                });
            }

            // 执行动态工具
            Object result = dynamicAgentTool.execute(args_.toArray());

            return R.data(result);
        }
    }
}
