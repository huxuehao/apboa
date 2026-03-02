package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;

/**
 * 描述：参数表
 *
 * @author huxuehao
 **/
@Data
@TableName("params")
public class Params implements SerializableEnable {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @QueryDefine(condition = QueryCondition.LIKE)
    private String paramName;

    @QueryDefine(condition = QueryCondition.LIKE)
    private String paramKey;

    @QueryDefine(condition = QueryCondition.LIKE)
    private String paramValue;
}
