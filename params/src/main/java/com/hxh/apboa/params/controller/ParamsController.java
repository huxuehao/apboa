package com.hxh.apboa.params.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.Params;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.params.service.ParamsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：系统参数
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/params")
public class ParamsController {
    private final ParamsService paramsService;

    public ParamsController(ParamsService paramsService) {
        this.paramsService = paramsService;
    }

    @GetMapping(value = "/page", name = "分页")
    public R<?> page(Params params, PageParams pageParams) {
        return R.data(paramsService.page(MP.getPage(pageParams), MP.getQueryWrapper(params)));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/add", name = "新增")
    public R<?> add(@RequestBody Params params) {
        return R.data(paramsService.saveV2(params));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/update", name = "编辑")
    public R<?> update(@RequestBody Params params) {
        return R.data(paramsService.updateByIdV2(params));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/delete", name = "删除")
    public R<?> delete(@RequestBody List<Long> ids) {
        return R.data(paramsService.removeBatchByIds(ids));
    }

    @GetMapping(value = "{id}", name = "根据ID唯一获取")
    public R<?> selectOne(@PathVariable("id") Long id) {
        return R.data(paramsService.getById(id));
    }

//    @GetMapping(value = "/fetch-value-by-key", name = "根据key获取value")
//    public R<?> fetchValueByKey(@RequestParam("key") String key) {
//        return R.data(paramsService.fetchValueByKey(key));
//    }

}
