package com.hxh.apboa.job.core.enums;

/**
 * 描述：枚举
 *
 * @author huxuehao
 **/
public enum QuartzEnum {
    IDENTITY_KEY("apboa_quartz_identity_key", "quart任务唯一标识"),
    RUN_MSG("runMsg", "任务执行信息，用户记录到日志表中");

    private final String value;
    private final String desc;

    public String value() {
        return value;
    }

    public String desc() {
        return desc;
    }

    QuartzEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
