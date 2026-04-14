package com.hxh.apboa.model.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.ModelProviderDTO;
import com.hxh.apboa.common.entity.ModelProvider;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.ModelProviderVO;
import com.hxh.apboa.model.service.ModelProviderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型提供商Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/model/provider")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderService modelProviderService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<ModelProviderVO>> page(PageParams pageParams, ModelProviderDTO query) {
        IPage<ModelProvider> page = modelProviderService.page(MP.getPage(pageParams), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, ModelProviderVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<ModelProviderVO> detail(@PathVariable("id") Long id) {
        ModelProvider entity = modelProviderService.getById(id);
        return R.data(BeanUtils.copy(entity, ModelProviderVO.class));
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody ModelProvider entity) {
        return R.data(modelProviderService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody ModelProvider entity) {
        return R.data(modelProviderService.doUpdate(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(modelProviderService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-model")
    public R<List<Object>> usedWithModel(@RequestBody List<Long> ids) {
        return R.data(modelProviderService.usedWithModel(ids));
    }
}
