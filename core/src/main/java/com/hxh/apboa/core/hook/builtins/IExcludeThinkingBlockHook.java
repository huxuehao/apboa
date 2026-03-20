package com.hxh.apboa.core.hook.builtins;

import com.hxh.apboa.core.hook.IAgentHook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ThinkingBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * 描述：过滤思考块
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class IExcludeThinkingBlockHook implements IAgentHook {
    @Override
    public String getDescription() {
        return "推理前排除历史消息思考内容Hook";
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent preReasoningEvent) {
            List<Msg> filteredMsgList = preReasoningEvent.getInputMessages()
                    .stream()
                    .map(msg -> {
                        boolean hasThing = msg.hasContentBlocks(ThinkingBlock.class);
                        if (hasThing) {
                            List<ContentBlock> filteredBlocks = msg.getContent()
                                    .stream()
                                    .filter(block -> !(block instanceof ThinkingBlock))
                                    .toList();

                            if (filteredBlocks.isEmpty()) {
                                return null;
                            }

                            return Msg.builder()
                                    .id(msg.getId())
                                    .role(msg.getRole())
                                    .name(msg.getName())
                                    .content(filteredBlocks)
                                    .metadata(msg.getMetadata())
                                    .build();
                        }
                        return msg;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            preReasoningEvent.setInputMessages(filteredMsgList);

            @SuppressWarnings("unchecked")
            T event_ = (T) preReasoningEvent;
            return Mono.just(event_);

        }
        return Mono.just(event);
    }
}
