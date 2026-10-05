package com.mallhygiene.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

/** 统一 JSON 错误响应 */
@Provider
public class ErrorMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

    @Override
    public Response toResponse(Exception e) {
        int status = 500;
        String message = e.getMessage();
        if (e instanceof WebApplicationException wae && wae.getResponse() != null) {
            status = wae.getResponse().getStatus();
        } else {
            LOG.error("未处理异常", e);
            if (message == null) {
                message = "服务器内部错误";
            }
        }
        return Response.status(status)
                .entity(Map.of("error", message == null ? "请求失败" : message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
