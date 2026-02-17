package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP服务器查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerDTO extends PageParams {

    @QueryDefine(value = "服务器名称", condition = QueryCondition.LIKE)
    private String name;

    @QueryDefine(value = "协议类型", condition = QueryCondition.EQ)
    private McpProtocol protocol;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
