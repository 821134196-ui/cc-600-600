package com.mall.tester.resource;

import com.mall.tester.dto.Dtos;
import com.mall.tester.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * 巡查记录查询接口。
 */
@Path("/api/patrols")
@Produces(MediaType.APPLICATION_JSON)
public class PatrolResource {

    @Inject
    TesterService service;

    @GET
    public List<Dtos.PatrolView> recent(@QueryParam("limit") @DefaultValue("50") int limit) {
        return service.recentPatrols(limit);
    }
}
