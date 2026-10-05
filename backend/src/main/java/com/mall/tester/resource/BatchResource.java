package com.mall.tester.resource;

import com.mall.tester.model.ProductBatch;
import com.mall.tester.dto.Dtos;
import com.mall.tester.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 正装批次接口。
 */
@Path("/api/batches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BatchResource {

    @Inject
    TesterService service;

    @GET
    public List<ProductBatch> list() {
        return service.listBatches();
    }

    @POST
    public ProductBatch create(Dtos.BatchRequest req) {
        return service.createBatch(req);
    }
}
