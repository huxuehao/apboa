package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词配置查询DTO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SensitiveWordConfigDTO extends PageParams {

    @QueryDefine(value = "分类", condition = QueryCondition.EQ)
    private String category;

    @QueryDefine(value = "配置名称", condition = QueryCondition.LIKE)
    private String name;

    @QueryDefine(value = "是否可用", condition = QueryCondition.EQ)
    private Boolean enabled;
}
