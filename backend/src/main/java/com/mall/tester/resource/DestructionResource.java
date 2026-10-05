package com.mall.tester.resource;

import com.mall.tester.dto.Dtos;
import com.mall.tester.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * 销毁接口：待销毁清单、销毁登记（含数量核对）、销毁记录、账实核对。
 */
@Path("/api/destructions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DestructionResource {

    @Inject
    TesterService service;

    @GET
    public List<Dtos.DestructionView> list() {
        return service.destructions();
    }

    @GET
    @Path("/pending")
    public List<Dtos.PendingDestructionView> pending() {
        return service.pendingDestruction();
    }

    @GET
    @Path("/reconcile")
    public Dtos.ReconcileView reconcile() {
        return service.reconcile();
    }

    @POST
    public Map<String, String> destroy(Dtos.DestroyRequest req) {
        String groupNo = service.destroy(req);
        return Map.of("groupNo", groupNo, "message", "销毁完成，共处理 " + req.itemCodes().size() + " 件");
    }
}
