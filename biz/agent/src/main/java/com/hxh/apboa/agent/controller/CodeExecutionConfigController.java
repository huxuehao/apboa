package com.hxh.apboa.agent.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.CodeExecutionConfig;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.agent.service.CodeExecutionConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：CodeExecutionConfigController
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/code-execution")
@RequiredArgsConstructor
public class CodeExecutionConfigController {
    private final CodeExecutionConfigService codeExecutionConfigService;

    /**
     * 分页查询
     */
    @GetMapping("/list")
    public R<List<CodeExecutionConfig>> list() {
        return R.data(codeExecutionConfigService.list());
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<CodeExecutionConfig> detail(@PathVariable("id") Long id) {
        return R.data(codeExecutionConfigService.getById(id));
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody CodeExecutionConfig entity) {
        return R.data(codeExecutionConfigService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody CodeExecutionConfig entity) {
        return R.data(codeExecutionConfigService.updateById(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(codeExecutionConfigService.removeByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(codeExecutionConfigService.usedWithAgent(ids));
    }
}
