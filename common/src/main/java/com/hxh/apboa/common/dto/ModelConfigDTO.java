package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型配置查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelConfigDTO extends PageParams {

    @QueryDefine(value = "提供商ID", condition = QueryCondition.EQ)
    private Long providerId;

    @QueryDefine(value = "模型名称", condition = QueryCondition.LIKE)
    private String name;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
