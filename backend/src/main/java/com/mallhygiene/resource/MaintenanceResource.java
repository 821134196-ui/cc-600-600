package com.mallhygiene.resource;

import com.mallhygiene.dto.Dtos.MockRecall;
import com.mallhygiene.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/** 运维操作：手动触发效期扫描（定时任务之外的手动入口，便于演示与测试） */
@Path("/api/maintenance")
@Produces(MediaType.APPLICATION_JSON)
public class MaintenanceResource {

    @Inject
    TesterService testerService;

    @POST
    @Path("/scan-expired")
    public Map<String, Object> scanExpired() {
        int n = testerService.removeExpiredItems();
        return Map.of("removed", n);
    }
}
