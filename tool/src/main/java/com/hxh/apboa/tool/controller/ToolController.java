package com.hxh.apboa.tool.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.ToolDTO;
import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.enums.ToolType;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.ToolVO;
import com.hxh.apboa.tool.service.ToolService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/tool")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<ToolVO>> page(ToolDTO query) {
        IPage<ToolConfig> page = toolService.page(MP.<ToolConfig>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, ToolVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<ToolVO> detail(@PathVariable("id") Long id) {
        ToolConfig entity = toolService.getById(id);

        ToolVO vo = BeanUtils.copy(entity, ToolVO.class);
        vo.setUsed(toolService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody ToolConfig entity) {
        entity.setToolType(ToolType.CUSTOM);
        return R.status(toolService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody ToolConfig entity) {
        return R.status(toolService.doUpdate(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.status(toolService.deleteTools(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(toolService.usedWithAgent(ids));
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/get/categories")
    public R<List<String>> listCategories() {
        return R.data(toolService.listCategories());
    }
}
