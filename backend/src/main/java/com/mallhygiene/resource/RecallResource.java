package com.mallhygiene.resource;

import com.mallhygiene.domain.RecallNotice;
import com.mallhygiene.dto.Dtos.MockRecall;
import com.mallhygiene.dto.Dtos.SyncResult;
import com.mallhygiene.service.RecallService;
import io.quarkus.panache.common.Sort;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 召回通知：查询已同步通知 + 从本地模拟接口同步 */
@Path("/api/recalls")
@Produces(MediaType.APPLICATION_JSON)
public class RecallResource {

    @Inject
    RecallService service;

    @GET
    public List<RecallNotice> list() {
        return RecallNotice.listAll(Sort.descending("publishedAt"));
    }

    /** 同步：拉取本地模拟接口的召回通知，涉及批次样品立即下架 */
    @POST
    @Path("/sync")
    public SyncResult sync() {
        List<MockRecall> remote = service.fetchRemote();
        return service.applyNotices(remote);
    }
}
