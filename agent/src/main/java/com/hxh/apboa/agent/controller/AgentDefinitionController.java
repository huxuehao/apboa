package com.hxh.apboa.agent.controller;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.AgentDefinitionDTO;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体定义Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/agent/definition")
@RequiredArgsConstructor
public class AgentDefinitionController {

    private final AgentDefinitionService agentDefinitionService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<AgentDefinitionVO>> page(AgentDefinitionDTO query) {
        IPage<AgentDefinition> page = agentDefinitionService.page(MP.<AgentDefinition>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, AgentDefinitionVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<AgentDefinitionVO> detail(@PathVariable("id") Long id) {
        AgentDefinitionVO vo = agentDefinitionService.agentDefinitionDetail(id);
        vo.setUsed(agentDefinitionService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody AgentDefinitionVO vo) {
        return R.status(agentDefinitionService.saveAgentDefinition(vo));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody AgentDefinitionVO vo) {
        return R.status(agentDefinitionService.updateAgentDefinition(vo));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.status(agentDefinitionService.deleteAgentDefinition(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(agentDefinitionService.usedWithAgent(ids));
    }

    /**
     * 获取所有Tag
     */
    @GetMapping("/get/tags")
    public R<List<String>> listTags() {
        return R.data(agentDefinitionService.listTags());
    }

    @GetMapping("/{id}/allow/file-type")
    public R<List<String>> allowFileType(@PathVariable("id") Long id) {
        return R.data(agentDefinitionService.allowFileType(id));
    }
}
