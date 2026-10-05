package com.mallhygiene.resource;

import com.mallhygiene.domain.InspectionRecord;
import com.mallhygiene.dto.Dtos.InspectionView;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 巡查记录查询 */
@Path("/api/inspections")
@Produces(MediaType.APPLICATION_JSON)
public class InspectionResource {

    @GET
    public List<InspectionView> recent(@QueryParam("limit") Integer limit) {
        int n = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        return InspectionRecord.<InspectionRecord>findAll(Sort.descending("inspectedAt"))
                .page(0, n)
                .list()
                .stream()
                .map(r -> new InspectionView(r.id, r.item.code, r.item.product.name, r.inspectedAt,
                        r.inspector, r.result.name(), r.note))
                .toList();
    }
}
