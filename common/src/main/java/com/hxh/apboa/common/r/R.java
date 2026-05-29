package com.hxh.apboa.common.r;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

/**
 * 描述：响应体包装类
 *
 * @author huxuehao
 **/
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // 状态码
    private Integer code;
    // 是否成功
    private boolean success;
    // 承载数据
    private T data;
    // 返回消息
    private String msg;

    private R(ResultCode resultCode) {
        this(resultCode, (T) null, resultCode.getMessage());
    }

    private R(ResultCode resultCode, String msg) {
        this(resultCode, (T) null, msg);
    }

    private R(ResultCode resultCode, T data) {
        this(resultCode, data, resultCode.getMessage());
    }

    private R(ResultCode resultCode, T data, String msg) {
        this(resultCode.getCode(), data, msg);
    }

    public R(T data) {
        this.code = ResultCode.SUCCESS.code;
        this.msg = ResultCode.SUCCESS.message;
        this.data = data;
    }

    public R(T data, String msg) {
        this.code = ResultCode.SUCCESS.code;
        this.msg = ResultCode.SUCCESS.message;
        this.data = data;
        this.msg = msg;
    }

    public R(Throwable e) {
        this.code = ResultCode.SUCCESS.code;
        this.msg = ResultCode.SUCCESS.message;
        this.msg = e.getMessage();
        this.code = ResultCode.FAILURE.code;
    }

    private R(int code, T data, String msg) {
        this.code = ResultCode.SUCCESS.code;
        this.msg = ResultCode.SUCCESS.message;
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.success = ResultCode.SUCCESS.code == code;
    }

    public static boolean isSuccess(R<?> result) {
        return Optional.ofNullable(result).map((x) -> nullSafeEquals(ResultCode.SUCCESS.code, x.code)).orElse(Boolean.FALSE);
    }

    public static boolean isNotSuccess(R<?> result) {
        return !isSuccess(result);
    }

    public static <T> R<T> data(T data) {
        return data(data, "操作成功");
    }

    public static <T> R<T> data(T data, String msg) {
        return data(200, data, msg);
    }

    public static <T> R<T> data(int code, T data, String msg) {
        return new R(code, data, data == null ? "暂无数据" : msg);
    }

    public static <T> R<T> success(String msg) {
        return new R<>(ResultCode.SUCCESS, msg);
    }

    public static <T> R<T> success(ResultCode resultCode) {
        return new R<>(resultCode);
    }

    public static <T> R<T> success(ResultCode resultCode, String msg) {
        return new R<>(resultCode, msg);
    }

    public static <T> R<T> fail(String msg) {
        return new R(ResultCode.FAILURE, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R(code, (Object)null, msg);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R(resultCode);
    }

    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R(resultCode, msg);
    }

    public static <T> R<T> status(boolean flag) {
        return flag ? success("操作成功") : fail("操作失败");
    }

    public int getCode() {
        return this.code;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public T getData() {
        return this.data;
    }

    public String getMsg() {
        return this.msg;
    }

    public R<T> setCode(final int code) {
        this.code = code;
        return this;
    }

    public R<T> setSuccess(final boolean success) {
        this.success = success;
        return this;
    }

    public R<T> setData(final T data) {
        this.data = data;
        return this;
    }

    public R<T> setMsg(final String msg) {
        this.msg = msg;
        return this;
    }

    public String toString() {
        return "R(code=" + this.getCode() + ", success=" + this.isSuccess() + ", data=" + this.getData() + ", msg=" + this.getMsg() + ")";
    }

    public R() {
        this.code = ResultCode.SUCCESS.code;
        this.msg = ResultCode.SUCCESS.message;
    }

    private static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else if (o1 != null && o2 != null) {
            if (o1.equals(o2)) {
                return true;
            } else {
                return o1.getClass().isArray() && o2.getClass().isArray() && arrayEquals(o1, o2);
            }
        } else {
            return false;
        }
    }

    private static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] && o2 instanceof Object[]) {
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        } else if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) o1, (boolean[]) o2);
        } else if (o1 instanceof byte[] && o2 instanceof byte[]) {
            return Arrays.equals((byte[]) o1, (byte[]) o2);
        } else if (o1 instanceof char[] && o2 instanceof char[]) {
            return Arrays.equals((char[]) o1, (char[]) o2);
        } else if (o1 instanceof double[] && o2 instanceof double[]) {
            return Arrays.equals((double[]) o1, (double[]) o2);
        } else if (o1 instanceof float[] && o2 instanceof float[]) {
            return Arrays.equals((float[]) o1, (float[]) o2);
        } else if (o1 instanceof int[] && o2 instanceof int[]) {
            return Arrays.equals((int[]) o1, (int[]) o2);
        } else if (o1 instanceof long[] && o2 instanceof long[]) {
            return Arrays.equals((long[]) o1, (long[]) o2);
        } else if (o1 instanceof short[] && o2 instanceof short[]) {
            return Arrays.equals((short[]) o1, (short[]) o2);
        }
        return false;
    }
}
