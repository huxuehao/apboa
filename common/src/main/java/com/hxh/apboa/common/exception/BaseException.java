package com.hxh.apboa.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 描述：基础异常
 *
 * @author huxuehao
 **/
@Getter
public class BaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String module;
    private String method;
    private String message;


    public BaseException(int code, String module, String method, String message) {
        this.code = code;
        this.module = module;
        this.method = method;
        this.message = message;
    }

    public BaseException(String module, String method, String message) {
        this.module = module;
        this.method = method;
        this.message = message;
    }

    public BaseException(String message) {
        this.message = message;
    }

    public BaseException() {
    }

}
