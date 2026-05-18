package com.hxh.apboa.mcp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.dto.McpToolEnabledDTO;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.vo.McpToolVO;
import java.util.List;

/**
 * MCP 服务器 Service
 *
 * @author huxuehao
 */
public interface McpServerService extends IService<McpServer> {
    List<Object> usedWithAgent(List<Long> ids);

    boolean deleteByIds(List<Long> ids);

    /**
     * 更新 MCP 服务器配置
     *
     * @param entity MCP 服务器配置
     * @return 更新后的配置
     */
    McpServer doUpdate(McpServer entity);

    /**
     * 手动激活 MCP
     *
     * @param id MCP ID
     * @return 激活后的配置
     */
    McpServer activate(Long id);

    /**
     * 手动同步工具目录
     *
     * @param id MCP ID
     * @return 同步后的配置
     */
    McpServer syncTools(Long id);

    List<McpToolVO> listTools(Long id);

    McpServer updateToolGlobalEnabled(Long id, McpToolEnabledDTO dto);
}
