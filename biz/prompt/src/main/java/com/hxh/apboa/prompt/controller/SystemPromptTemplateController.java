package com.hxh.apboa.prompt.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.SystemPromptTemplateDTO;
import com.hxh.apboa.common.entity.SystemPromptTemplate;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.SystemPromptTemplateVO;
import com.hxh.apboa.prompt.service.SystemPromptTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统提示词模板Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/prompt/template")
@RequiredArgsConstructor
public class SystemPromptTemplateController {

    private final SystemPromptTemplateService systemPromptTemplateService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<SystemPromptTemplateVO>> page(SystemPromptTemplateDTO query) {
        IPage<SystemPromptTemplate> page = systemPromptTemplateService.page(MP.<SystemPromptTemplate>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, SystemPromptTemplateVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<SystemPromptTemplateVO> detail(@PathVariable("id") Long id) {
        SystemPromptTemplate entity = systemPromptTemplateService.getById(id);

        SystemPromptTemplateVO vo = BeanUtils.copy(entity, SystemPromptTemplateVO.class);
        vo.setUsed(systemPromptTemplateService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody SystemPromptTemplate entity) {
        return R.data(systemPromptTemplateService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody SystemPromptTemplate entity) {
        return R.data(systemPromptTemplateService.doUpdate(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(systemPromptTemplateService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(systemPromptTemplateService.usedWithAgent(ids));
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/get/categories")
    public R<List<String>> listCategories() {
        return R.data(systemPromptTemplateService.listCategories());
    }
}
