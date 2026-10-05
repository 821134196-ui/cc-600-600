package com.mallhygiene.service;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/** 业务冲突（如状态不允许的操作、重复销毁），映射为 HTTP 409 */
public class ConflictException extends ClientErrorException {

    public ConflictException(String message) {
        super(message, Response.Status.CONFLICT);
    }
}
