package com.mall.tester.service;

import jakarta.ws.rs.core.Response;

/**
 * 业务异常：携带 HTTP 状态码，由 BizExceptionMapper 统一转成 JSON 错误响应。
 */
public class BizException extends RuntimeException {

    private final int status;

    public BizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static BizException badRequest(String msg) {
        return new BizException(Response.Status.BAD_REQUEST.getStatusCode(), msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(Response.Status.NOT_FOUND.getStatusCode(), msg);
    }

    public static BizException conflict(String msg) {
        return new BizException(Response.Status.CONFLICT.getStatusCode(), msg);
    }

    public int status() {
        return status;
    }
}
