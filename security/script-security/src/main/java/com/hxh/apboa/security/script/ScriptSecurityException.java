package com.hxh.apboa.security.script;

/**
 * 描述：脚本安全检查过程中抛出的异常
 *
 * @author huxuehao
 */
public class ScriptSecurityException extends RuntimeException {

    public ScriptSecurityException(String message) {
        super(message);
    }

    public ScriptSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

}
