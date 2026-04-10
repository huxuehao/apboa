package com.hxh.apboa.agent;

import com.hxh.apboa.agent.service.AgentChatKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 描述：初始化API Key
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatKeyInit implements ApplicationRunner {
    private final AgentChatKeyService agentChatKeyService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            agentChatKeyService.list().forEach(agentChatKey -> {
                String chatKey = agentChatKey.getChatKey();
                agentChatKeyService.getAgentCodeByChatKey(chatKey);
            });
            log.info("Chat Key 初始化完成");
        } catch (Exception e) {
            log.error("Chat Key 初始化失败", e);
            throw new RuntimeException(e);
        }
    }
}
