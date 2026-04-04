package com.hxh.apboa.a2a.controller;

import com.hxh.apboa.a2a.service.AgentA2aService;
import com.hxh.apboa.common.entity.AgentA2A;
import com.hxh.apboa.common.r.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 描述：AgentA2aController
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/agentA2a")
@RequiredArgsConstructor
public class AgentA2aController {
    private final AgentA2aService agentA2aService;

    /**
     * 保存A2A配置
     */
    @PostMapping
    public R<Boolean> saveA2aConfig(@RequestBody AgentA2A agentA2A) {
        return R.data(agentA2aService.saveA2aConfig(agentA2A));
    }
    /**
     * 根据agentId获取A2A配置
     */
    @GetMapping("/{agentId}")
    public R<AgentA2A> getA2aConfigByAgentId(@PathVariable("agentId") Long agentId) {
        return R.data(agentA2aService.getA2aConfigByAgentId(agentId));
    }

}
