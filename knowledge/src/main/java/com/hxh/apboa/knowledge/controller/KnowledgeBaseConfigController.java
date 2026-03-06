package com.hxh.apboa.knowledge.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.KnowledgeBaseConfigDTO;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.KnowledgeBaseConfigVO;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库配置Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/knowledge/config")
@RequiredArgsConstructor
public class KnowledgeBaseConfigController {

    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<KnowledgeBaseConfigVO>> page(KnowledgeBaseConfigDTO query) {
        IPage<KnowledgeBaseConfig> page = knowledgeBaseConfigService.page(MP.<KnowledgeBaseConfig>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, KnowledgeBaseConfigVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<KnowledgeBaseConfigVO> detail(@PathVariable("id") Long id) {
        KnowledgeBaseConfig entity = knowledgeBaseConfigService.getById(id);

        KnowledgeBaseConfigVO vo = BeanUtils.copy(entity, KnowledgeBaseConfigVO.class);
        vo.setUsed(knowledgeBaseConfigService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody KnowledgeBaseConfig entity) {
        return R.data(knowledgeBaseConfigService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody KnowledgeBaseConfig entity) {
        return R.data(knowledgeBaseConfigService.updateById(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(knowledgeBaseConfigService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(knowledgeBaseConfigService.usedWithAgent(ids));
    }
}
