package com.hxh.apboa.core.model.checkpiont;

/**
 * 描述：模型检查结果
 *
 * @author huxuehao
 **/
public class CheckModelResult {
    public Boolean success;
    public String message;

    public CheckModelResult() {}

    public CheckModelResult(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
