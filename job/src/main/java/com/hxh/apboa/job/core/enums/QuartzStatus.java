package com.hxh.apboa.job.core.enums;

/**
 * 描述：枚举
 *
 * @author huxuehao
 **/
public enum QuartzStatus {
    REMOVE(false, "删除"),
    START(true, "开始（调度）");
    private final Boolean value;
    private final String desc;

    public boolean value() {
        return value;
    }

    public String desc() {
        return desc;
    }

    QuartzStatus(boolean value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
