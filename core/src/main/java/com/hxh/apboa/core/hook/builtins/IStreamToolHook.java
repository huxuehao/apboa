package com.hxh.apboa.core.hook.builtins;

import ch.qos.logback.core.spi.ConfigurationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.hook.IAgentHook;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 描述：
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class IStreamToolHook implements IAgentHook {
    @Override
    public String getDescription() {
        return "处理工具流式响应Hook";
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
//        if (event instanceof ActingChunkEvent actingChunkEvent) {
//            for (ContentBlock block : actingChunkEvent.getChunk().getOutput()) {
//                if (block instanceof TextBlock textBlock) {
//                    JsonNode text = JsonUtils.parse(textBlock.getText());
//                    boolean last = text.get("last").asBoolean();
//                    if (last) {
//                        continue;
//                    }
//                    EventType type = EventType.valueOf(text.get("type").asText());
//                    JsonNode message = text.get("message");
//                    Event msgEvent = new Event(
//                            type,
//                            JsonUtils.parse(JsonUtils.toJsonStr(message), Msg.class),
//                            false);




//                    JsonNode message = text.get("message");
//                    if (message == null) {
//                        continue;
//                    }
//                    JsonNode content = message.get("content");
//                    if (content == null || !content.isArray()) {
//                        continue;
//                    }
//                    ArrayNode arrayNode = (ArrayNode) content;
//                    for (JsonNode jsonNode : arrayNode) {
//                        if (!"text".equals(jsonNode.get("type").asText())) {
//                            continue;
//                        }
//                        String token = jsonNode.get("text").asText();
//
//                    }




//                }
//            }
//        }
        return Mono.just(event);
    }
}
