package com.hxh.apboa.mcp.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.McpServerDTO;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.McpServerVO;
import com.hxh.apboa.mcp.service.McpServerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP服务器Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/mcp/server")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<McpServerVO>> page(McpServerDTO query) {
        IPage<McpServer> page = mcpServerService.page(MP.<McpServer>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, McpServerVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<McpServerVO> detail(@PathVariable("id") Long id) {
        McpServer entity = mcpServerService.getById(id);

        McpServerVO vo = BeanUtils.copy(entity, McpServerVO.class);
        vo.setUsed(mcpServerService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody McpServer entity) {
        return R.data(mcpServerService.save(entity));
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody McpServer entity) {
        return R.data(mcpServerService.doUpdate(entity));
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(mcpServerService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(mcpServerService.usedWithAgent(ids));
    }
}
