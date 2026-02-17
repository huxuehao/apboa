package com.hxh.apboa.common.exception;

/**
 * 描述：请求未授权异常
 *
 * @author huxuehao
 **/
public class NotAuthException extends BaseException {
    public NotAuthException(int code, String module, String method, String message) {
        super(code, module, method, message);
    }

    public NotAuthException(String module, String method, String message) {
        super(404, module, method, message);
    }

    public NotAuthException(String message) {
        super(401, null, null, message);
    }

    public NotAuthException() {
        super(401, null, null, "访问未授权");
    }
}
