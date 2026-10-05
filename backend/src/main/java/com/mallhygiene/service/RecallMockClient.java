package com.mallhygiene.service;

import com.mallhygiene.dto.Dtos.MockRecall;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/** 召回通知本地模拟接口的客户端 */
@Path("/mock-api/recalls")
@RegisterRestClient(configKey = "recall-mock")
public interface RecallMockClient {

    @GET
    List<MockRecall> list();
}
