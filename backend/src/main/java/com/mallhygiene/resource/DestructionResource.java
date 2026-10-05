package com.mallhygiene.resource;

import com.mallhygiene.domain.DestructionBatch;
import com.mallhygiene.dto.Dtos.DestroyReq;
import com.mallhygiene.dto.Dtos.ReconcileView;
import com.mallhygiene.service.TesterService;
import io.quarkus.panache.common.Sort;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 销毁管理：登记销毁（数量/原因/经手人）+ 数量核对 */
@Path("/api/destructions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DestructionResource {

    @Inject
    TesterService service;

    @GET
    public List<DestructionBatch> list() {
        return DestructionBatch.listAll(Sort.descending("destroyedAt"));
    }

    @POST
    public DestructionBatch destroy(DestroyReq req) {
        return service.destroy(req.itemIds(), req.reason(), req.handler());
    }

    /** 核对：申报数 = 实际处理数 = 库内已销毁件数 */
    @GET
    @Path("/reconcile")
    public ReconcileView reconcile() {
        return service.reconcile();
    }
}
