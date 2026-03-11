package com.hxh.apboa.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.agent.mapper.IJobInfoMapper;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.AgentDefinitionDTO;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.JobInfo;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final IJobInfoMapper iJobInfoMapper;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<AgentDefinitionVO>> page(AgentDefinitionDTO query) {
        IPage<AgentDefinition> page = agentDefinitionService.page(MP.<AgentDefinition>getPage(query), MP.getQueryWrapper(query));
        IPage<AgentDefinitionVO> pageVo = BeanUtils.copyPage(page, AgentDefinitionVO.class);
        List<JobInfo> agent = iJobInfoMapper.selectList(new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getType, "AGENT"));
        if (!agent.isEmpty()) {
            Map<String, JobInfo> collectMap = agent.stream().collect(Collectors.toMap(
                    JobInfo::getBizId,
                    item -> item,
                    (existing, replacement) -> existing));
            pageVo.getRecords().forEach(agentVo -> {
                if (collectMap.containsKey(String.valueOf(agentVo.getId()))) {
                    agentVo.setJobInfo(collectMap.get(String.valueOf(agentVo.getId())));
                }
            });
        }

        return R.data(pageVo);
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<AgentDefinitionVO> detail(@PathVariable("id") Long id) {
        AgentDefinitionVO vo = agentDefinitionService.agentDefinitionDetail(id);
        vo.setUsed(agentDefinitionService.usedWithAgent(List.of(id)));
        List<JobInfo> agent = iJobInfoMapper.selectList(
                new LambdaQueryWrapper<JobInfo>()
                        .eq(JobInfo::getType, "AGENT")
                        .eq(JobInfo::getBizId, String.valueOf(id)));
        if (agent.size() == 1) {
            vo.setJobInfo(agent.getFirst());
        }

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody AgentDefinitionVO vo) {
        return R.data(agentDefinitionService.saveAgentDefinition(vo));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody AgentDefinitionVO vo) {
        return R.data(agentDefinitionService.updateAgentDefinition(vo));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(agentDefinitionService.deleteAgentDefinition(ids));
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
