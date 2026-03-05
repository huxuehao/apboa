package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体定义查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentDefinitionDTO extends PageParams {

    @QueryDefine(value = "智能体名称", condition = QueryCondition.LIKE)
    private String name;

    @QueryDefine(value = "智能体类型", condition = QueryCondition.EQ)
    private AgentType agentType;

    @QueryDefine(value = "智能体代码", condition = QueryCondition.EQ)
    private String agentCode;


    @QueryDefine(value = "智能体标签", condition = QueryCondition.EQ)
    private String tag;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
