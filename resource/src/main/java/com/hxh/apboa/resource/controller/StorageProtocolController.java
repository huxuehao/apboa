package com.hxh.apboa.resource.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.StorageProtocol;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.resource.service.StorageProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：文件存储协议配置
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageProtocolController {
    private final StorageProtocolService storageProtocolService;

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/add", name = "新增")
    public R<Boolean> add(@RequestBody StorageProtocol body) {
        return R.data(storageProtocolService.saveV2(body));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/update", name = "编辑")
    public R<Boolean> update(@RequestBody StorageProtocol body) {
        return R.data(storageProtocolService.updateV2(body));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/delete", name = "删除")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(storageProtocolService.removeBatchByIds(ids));
    }

    @GetMapping(value = "/page", name = "分页")
    public R<IPage<StorageProtocol>> page(StorageProtocol storageProtocol, PageParams pageParams) {
        return R.data(storageProtocolService.page(MP.getPage(pageParams), MP.getQueryWrapper(storageProtocol)));
    }

    @GetMapping(value = "/selectOne", name = "根据ID唯一获取")
    public R<StorageProtocol> selectOne(@RequestParam Long id) {
        return R.data(storageProtocolService.getById(id));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @GetMapping(value = "/validSuccess", name = "设置有效")
    public R<Boolean> validSuccess(@RequestParam Long id) {
        return R.data(storageProtocolService.validSuccess(id));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/updateProtocol", name = "更新协议配置")
    public R<Boolean> updateProtocol(@RequestBody StorageProtocol body) {
        UpdateWrapper<StorageProtocol> uw = new UpdateWrapper<>();
        uw.eq("id", body.getId());
        uw.set("protocol_config", body.getProtocolConfig());
        return R.data(storageProtocolService.update(uw));
    }
}
