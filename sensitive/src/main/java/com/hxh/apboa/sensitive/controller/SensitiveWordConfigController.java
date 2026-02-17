package com.hxh.apboa.sensitive.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.SensitiveWordConfigDTO;
import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.SensitiveWordConfigVO;
import com.hxh.apboa.sensitive.service.SensitiveWordConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 敏感词配置Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/sensitive/config")
@RequiredArgsConstructor
public class SensitiveWordConfigController {

    private final SensitiveWordConfigService sensitiveWordConfigService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<SensitiveWordConfigVO>> page(SensitiveWordConfigDTO query) {
        IPage<SensitiveWordConfig> page = sensitiveWordConfigService.page(MP.<SensitiveWordConfig>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, SensitiveWordConfigVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<SensitiveWordConfigVO> detail(@PathVariable("id") Long id) {
        SensitiveWordConfig entity = sensitiveWordConfigService.getById(id);

        SensitiveWordConfigVO vo = BeanUtils.copy(entity, SensitiveWordConfigVO.class);
        vo.setUsed(sensitiveWordConfigService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody SensitiveWordConfig entity) {
        return R.status(sensitiveWordConfigService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody SensitiveWordConfig entity) {
        return R.status(sensitiveWordConfigService.updateById(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.status(sensitiveWordConfigService.removeByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(sensitiveWordConfigService.usedWithAgent(ids));
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/get/categories")
    public R<List<String>> listCategories() {
        return R.data(sensitiveWordConfigService.listCategories());
    }
}
