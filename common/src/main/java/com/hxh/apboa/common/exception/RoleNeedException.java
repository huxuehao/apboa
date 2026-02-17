package com.hxh.apboa.common.exception;

/**
 * 描述：无权访问异常
 *
 * @author huxuehao
 **/
public class RoleNeedException extends BaseException {
    public RoleNeedException(int code, String module, String method, String message) {
        super(code, module, method, message);
    }

    public RoleNeedException(String module, String method, String message) {
        super(404, module, method, message);
    }

    public RoleNeedException(String message) {
        super(400, null, null, message);
    }

    public RoleNeedException() {
        super(400, null, null, "无权访问");
    }
}
