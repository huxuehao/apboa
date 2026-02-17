package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.enums.ToolType;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolDTO extends PageParams {

    @QueryDefine(value = "工具名称", condition = QueryCondition.LIKE)
    private String name;

    @QueryDefine(value = "工具编号", condition = QueryCondition.EQ)
    private String toolId;

    @QueryDefine(value = "工具类型", condition = QueryCondition.EQ)
    private ToolType toolType;

    @QueryDefine(value = "工具分类", condition = QueryCondition.EQ)
    private String category;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
