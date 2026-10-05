package com.mall.tester.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;

/**
 * 本地模拟接口：模拟外部召回通知系统。
 * 数据来自配置文件指定的 JSON 文件（默认 data/recall-notices.json），
 * 修改文件后再次同步即可模拟新的召回通报。
 */
@Path("/mock-api")
public class MockRecallResource {

    @ConfigProperty(name = "tester.recall.mock-file")
    String mockFile;

    @GET
    @Path("/recall-notices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recallNotices() throws Exception {
        java.nio.file.Path file = java.nio.file.Path.of(mockFile);
        if (!Files.exists(file)) {
            return Response.ok("[]").build();
        }
        return Response.ok(Files.readString(file)).build();
    }
}
