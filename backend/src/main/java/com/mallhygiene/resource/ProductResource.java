package com.mallhygiene.resource;

import com.mallhygiene.domain.Product;
import com.mallhygiene.dto.Dtos.ProductReq;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/** 正装商品批次 */
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<Product> list() {
        return Product.listAll(Sort.by("batchNo"));
    }

    @POST
    @Transactional
    public Response create(ProductReq req) {
        if (req == null || req.name() == null || req.name().isBlank()
                || req.batchNo() == null || req.batchNo().isBlank()) {
            throw new BadRequestException("商品名称与批次号不能为空");
        }
        if (Product.count("batchNo", req.batchNo()) > 0) {
            throw new com.mallhygiene.service.ConflictException("批次号已存在: " + req.batchNo());
        }
        Product p = new Product();
        p.name = req.name();
        p.brand = req.brand() == null ? "" : req.brand();
        p.batchNo = req.batchNo();
        p.productionDate = req.productionDate();
        p.shelfMonths = req.shelfMonths() == null ? 36 : req.shelfMonths();
        p.paoMonths = req.paoMonths() == null ? 6 : req.paoMonths();
        p.persist();
        return Response.status(Response.Status.CREATED).entity(p).build();
    }
}
