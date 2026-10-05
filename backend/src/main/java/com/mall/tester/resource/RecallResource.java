package com.mall.tester.resource;

import com.mall.tester.dto.Dtos;
import com.mall.tester.model.RecallNotice;
import com.mall.tester.service.RecallService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 召回接口：从本地模拟接口同步召回通知。
 */
@Path("/api/recalls")
@Produces(MediaType.APPLICATION_JSON)
public class RecallResource {

    @Inject
    RecallService recallService;

    @GET
    public List<RecallNotice> list() {
        return recallService.listSynced();
    }

    @POST
    @Path("/sync")
    public Dtos.RecallSyncResult sync() {
        return recallService.syncFromMock();
    }
}
