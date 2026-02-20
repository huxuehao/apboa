package com.hxh.apboa.mcp.service;

import com.hxh.apboa.common.entity.McpServer;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * MCP服务器Service
 *
 * @author huxuehao
 */
public interface McpServerService extends IService<McpServer> {
    List<Object> usedWithAgent(List<Long> ids);
    boolean deleteByIds(List<Long> ids);
}
