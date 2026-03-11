package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.QueryCondition;
import lombok.Data;

/**
 * 描述：quartz执行状态实体类
 * @author huxuehao
 **/
@Data
@TableName("quartz_job_info")
public class JobInfo implements SerializableEnable {
	/**定时任务标识*/
    private String id;
    /*关联业务ID*/
    @QueryDefine(value = "类型", condition = QueryCondition.EQ)
    private String type;
    /*关联业务ID*/
    private String bizId;
    /*corn*/
    private String cron;
    /*执行类*/
    private String jobClass;
    /*执行参数*/
    private String dataMap;
	/**是否启用*/
    private boolean enabled;
}
