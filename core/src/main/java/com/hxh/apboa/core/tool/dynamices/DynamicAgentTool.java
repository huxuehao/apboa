package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.common.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 描述：动态工具构建类
 *
 * @author huxuehao
 **/
public class DynamicAgentTool implements AgentTool {
    private final ToolConfig toolConfig;

    public DynamicAgentTool(ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }

    @Override
    public String getName() {
        return toolConfig.getToolId();
    }

    @Override
    public String getDescription() {
        return toolConfig.getDescription();
    }

    @Override
    public Map<String, Object> getParameters() {
        // 获取输入参数的 JSON Schema
        JsonNode inputSchema = toolConfig.getInputSchema();
        if (inputSchema == null || inputSchema.isNull()) {
            return Map.of();
        }

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        // 构建 properties 和 required
        for (JsonNode jsonNode : inputSchema) {
            String name = JsonUtils.getStringValue(jsonNode, "name", true);
            String type = JsonUtils.getStringValue(jsonNode, "type", true);
            String description = JsonUtils.getStringValue(jsonNode, "description", false);
            String defaultValue = JsonUtils.getStringValue(jsonNode, "defaultValue", false);
            boolean required_ = JsonUtils.getBooleanValue(jsonNode, "required", false);

            properties.put(name, new HashMap<>(){{
                put("type", type);

                if (!FuncUtils.isEmpty(description)) {
                    put("description", description);
                } else {
                    put("description", name);
                }
                if (!FuncUtils.isEmpty(defaultValue)) {
                    put("defaultValue", defaultValue);
                }
            }});

            if (required_) {
                required.add(name);
            }
        }

        return new HashMap<>() {{
            put("type", "object");
            put("properties", properties);
            put("required", required);
        }};
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            try {
                // 获取工具执行参数
                List<Object> args = getObjects(param);

                // 拿到动态工具的实例
                IDynamicAgentTool dynamicAgentTool = ToolInstanceLoadFactory
                        .getInstanceLoader(toolConfig.getLanguage()).loadInstance(toolConfig.getCode());

                // 执行动态工具
                Object result = dynamicAgentTool.execute(args.toArray());

                return ToolResultBlock.of(
                        param.getToolUseBlock().getId(),
                        param.getToolUseBlock().getName(),
                        TextBlock.builder().text(JsonUtils.toJsonStr(R.data(result))).build()
                );
            } catch (Exception e) {
                return ToolResultBlock.of(
                        param.getToolUseBlock().getId(),
                        param.getToolUseBlock().getName(),
                        TextBlock.builder().text(JsonUtils.toJsonStr(R.fail(e.getMessage()))).build()
                );
            }
        });
    }

    /**
     * 获取工具执行参数
     *
     * @param param 工具调用参数
     * @return 工具执行参数
     */
    private List<Object> getObjects(ToolCallParam param) {
        List<Object> args = new LinkedList<>();
        Map<String, Object> input = param.getInput();
        JsonNode inputSchema = toolConfig.getInputSchema();

        if (input != null && !input.isEmpty() && inputSchema != null && !inputSchema.isEmpty()) {
            inputSchema.forEach(jsonNode -> {
                if (jsonNode.has("name") && !jsonNode.get("name").asText().trim().isEmpty()) {
                    String name = jsonNode.get("name").textValue();
                    args.add(input.getOrDefault(name, null));
                }
            });
        }
        return args;
    }
}
