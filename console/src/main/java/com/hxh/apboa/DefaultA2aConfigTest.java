//package com.hxh.apboa;
//
//import com.hxh.apboa.agent.service.AgentDefinitionService;
//import com.hxh.apboa.common.entity.AgentDefinition;
//import com.hxh.apboa.core.agent.ReActAgentHelper;
//import com.hxh.apboa.core.model.ChatModelFactory;
//import io.agentscope.core.ReActAgent;
//import io.agentscope.core.memory.InMemoryMemory;
//import io.agentscope.core.memory.Memory;
//import io.agentscope.core.model.Model;
//import io.agentscope.core.tool.Toolkit;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 描述：注册 ReactAgent 到 nacos 测试
// *
// * @author huxuehao
// **/
//@Configuration
//@RequiredArgsConstructor
//public class DefaultA2aConfig {
//    private final ReActAgentHelper ReActAgentHelper;
//    private final ChatModelFactory chatModelFactory;
//    private final AgentDefinitionService agentDefinitionService;
//    @Bean
//    public Model myModel() {
//        AgentDefinition definition = agentDefinitionService.getById("2028837794948431874");
//        return chatModelFactory.getModel(definition);
//    }
//    @Bean
//    public Memory myMemory() {
//        return new InMemoryMemory();
//    }
//    @Bean
//    public Toolkit myToolkit() {
//        return new Toolkit();
//    }
//    @Bean
//    public ReActAgent.Builder myReActAgentBuilder() {
//        return ReActAgentHelper.getReActAgentBuilder(2028837794948431874L);
//    }
//}
