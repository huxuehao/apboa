package com.hxh.apboa.security.script.model;

/**
 * 描述：不安全行为的严重等级
 *
 * @author huxuehao
 */
public enum Severity {

    /** 可疑但通常无害 */
    LOW,

    /** 有潜在风险 */
    MEDIUM,

    /** 高风险 */
    HIGH,

    /** 明确攻击行为*/
    CRITICAL

}
