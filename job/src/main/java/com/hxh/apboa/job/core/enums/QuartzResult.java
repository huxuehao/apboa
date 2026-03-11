package com.hxh.apboa.job.core.enums;

/**
 * 描述：枚举
 *
 * @author huxuehao
 **/
public enum QuartzResult {
    STATUS_SUCCESS("1", "执行成功标记"),
    STATUS_FAIL("0", "执行失败标记");

    private final String value;
    private final String desc;

    public String value() {
        return value;
    }

    public String desc() {
        return desc;
    }

    QuartzResult(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
