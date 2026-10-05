package com.mallhygiene.resource;

import com.mallhygiene.domain.InspectionResult;
import com.mallhygiene.dto.Dtos.InspectReq;
import com.mallhygiene.dto.Dtos.InspectionView;
import com.mallhygiene.dto.Dtos.IssueReq;
import com.mallhygiene.dto.Dtos.ItemView;
import com.mallhygiene.dto.Dtos.ReasonReq;
import com.mallhygiene.dto.Dtos.StockInReq;
import com.mallhygiene.dto.Dtos.TrailView;
import com.mallhygiene.dto.Dtos.TransferReq;
import com.mallhygiene.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 试用品：列表、领用、调柜、冻结、巡查、下架、履历 */
@Path("/api/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TesterResource {

    @Inject
    TesterService service;

    @GET
    public List<ItemView> list(@QueryParam("status") String status,
                               @QueryParam("counter") String counter,
                               @QueryParam("q") String q) {
        return service.listItems(status, counter, q);
    }

    @GET
    @Path("/{code}/trail")
    public TrailView trail(@PathParam("code") String code) {
        return service.trail(code);
    }

    @POST
    @Path("/stock-in")
    public ItemView stockIn(StockInReq req) {
        return service.stockIn(req == null ? null : req.code(),
                req == null ? null : req.productId(),
                req == null ? null : req.operator());
    }

    @POST
    @Path("/{code}/issue")
    public ItemView issue(@PathParam("code") String code, IssueReq req) {
        return service.issue(code, req.openedDate(), req.counterLocation(), req.operator());
    }

    @POST
    @Path("/{code}/transfer")
    public ItemView transfer(@PathParam("code") String code, TransferReq req) {
        return service.transfer(code, req.toLocation(), req.operator());
    }

    @POST
    @Path("/{code}/freeze")
    public ItemView freeze(@PathParam("code") String code, ReasonReq req) {
        return service.freeze(code, req == null ? null : req.reason(), req == null ? null : req.operator());
    }

    @POST
    @Path("/{code}/unfreeze")
    public ItemView unfreeze(@PathParam("code") String code, ReasonReq req) {
        return service.unfreeze(code, req == null ? null : req.operator());
    }

    @POST
    @Path("/{code}/remove")
    public ItemView remove(@PathParam("code") String code, ReasonReq req) {
        return service.remove(code, req == null ? null : req.reason(), req == null ? null : req.operator());
    }

    @POST
    @Path("/{code}/inspect")
    public InspectionView inspect(@PathParam("code") String code, InspectReq req) {
        InspectionResult result;
        try {
            result = InspectionResult.valueOf(req.result());
        } catch (Exception e) {
            throw new BadRequestException("非法巡查结论: " + req.result());
        }
        return service.inspect(code, result, req.inspector(), req.note());
    }
}
