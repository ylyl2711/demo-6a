package com.project.stockwork.common;

import lombok.Data;

@Data
public class CommonResult<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> CommonResult<T> ok() {
        return ok(null);
    }

    public static <T> CommonResult<T> ok(T data) {
        CommonResult<T> r = new CommonResult<>();
        r.setCode(200);
        r.setMsg("成功");
        r.setData(data);
        return r;
    }

    public static <T> CommonResult<T> fail(int code, String msg) {
        CommonResult<T> r = new CommonResult<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> CommonResult<T> fail(String msg) {
        return fail(400, msg);
    }
}
