package com.hxh.apboa.skill.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.SkillPackageDTO;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.SkillPackageVO;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 技能包Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillPackageController {

    private final SkillPackageService skillPackageService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<SkillPackageVO>> page(SkillPackageDTO query) {
        IPage<SkillPackage> page = skillPackageService.page(MP.<SkillPackage>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, SkillPackageVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<SkillPackageVO> detail(@PathVariable("id") Long id) {
        SkillPackage entity = skillPackageService.getById(id);

        SkillPackageVO vo = BeanUtils.copy(entity, SkillPackageVO.class);
        vo.setUsed(skillPackageService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody SkillPackage entity) {
        return R.data(skillPackageService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody SkillPackage entity) {
        return R.data(skillPackageService.updateById(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(skillPackageService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(skillPackageService.usedWithAgent(ids));
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/get/categories")
    public R<List<String>> listCategories() {
        return R.data(skillPackageService.listCategories());
    }
}
