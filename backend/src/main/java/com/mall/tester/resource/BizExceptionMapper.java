package com.mall.tester.resource;

import com.mall.tester.service.BizException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * 业务异常 -> JSON 错误响应。
 */
@Provider
public class BizExceptionMapper implements ExceptionMapper<BizException> {

    @Override
    public Response toResponse(BizException e) {
        return Response.status(e.status())
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", e.getMessage()))
                .build();
    }
}
