package com.hxh.apboa.common.mp.support;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import lombok.Getter;

/**
 * 描述：查询条件
 *
 * @author huxuehao
 */
@Getter
public enum QueryCondition {
    GT(">", "gt", "大于"),
    GE(">=", "ge", "大于等于"),
    LT("<", "lt", "小于"),
    LE("<=", "le", "小于等于"),
    EQ("=", "eq", "等于"),
    NE("!=", "ne", "不等于"),
    IN("IN", "in", "包含"),
    LIKE("LIKE", "like", "全模糊"),
    LEFT_LIKE("LEFT_LIKE", "likeleft", "左模糊"),
    RIGHT_LIKE("RIGHT_LIKE", "likeright", "右模糊"),
    SQL_RULES("USE_SQL_RULES", "ext", "自定义SQL片段"),
    IS_NULL("is null", "null", ""),
    IS_NOT_NULL("is not null", "notnull", "is not null"),
    HAVING("having", "having", "having"),
    EXISTS("exists", "exists", "exists"),
    BETWEEN("between", "between", "between");

    private final String value;
    private final String condition;
    private final String msg;

    QueryCondition(String value, String condition, String msg) {
        this.value = value;
        this.condition = condition;
        this.msg = msg;
    }

    public static QueryCondition getByValue(String value) {
        if (ObjectUtils.isNotEmpty(value)) {
            for (QueryCondition sqlOperator : values()) {
                if (sqlOperator.getValue().equals(value) || sqlOperator.getCondition().equals(value)) {
                    return sqlOperator;
                }
            }
        }
        return null;
    }
}
