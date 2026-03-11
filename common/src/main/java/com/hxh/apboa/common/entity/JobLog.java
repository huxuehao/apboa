package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 描述：quartz日志实体类
 * @author huxuehao
 **/
@Data
@TableName("quartz_job_log")
public class JobLog implements SerializableEnable {

	/**定时任务日志主键*/
	@TableId(type = IdType.ASSIGN_ID)
    private String id;
	/**任务标识*/
    private String identity;
    /**开始时间*/
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**结束时间*/
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**执行情况*/
    private String content;
	/**状态*/
    private String status;
	/**持续时间,单位:秒*/
    private Long duration;

    public JobLog() {
    }

    public JobLog(String identity, Date startTime, Date endTime, Long duration, String content, String status) {
        this.identity = identity;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.content = content;
        this.status = status;
    }
}
