package com.mallhygiene.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallhygiene.dto.Dtos.MockRecall;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 召回通知本地模拟接口：模拟品牌方/监管端的通告服务。
 * 数据来自 classpath:mock-recalls.json，业务侧通过 REST 客户端拉取。
 */
@Path("/mock-api/recalls")
@Produces(MediaType.APPLICATION_JSON)
public class MockRecallResource {

    @Inject
    ObjectMapper objectMapper;

    private List<MockRecall> notices;

    @PostConstruct
    void load() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/mock-recalls.json")) {
            notices = Arrays.asList(objectMapper.readValue(in, MockRecall[].class));
        }
    }

    @GET
    public List<MockRecall> list() {
        return notices;
    }
}
