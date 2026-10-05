package com.mall.tester.resource;

import com.mall.tester.dto.Dtos;
import com.mall.tester.model.TesterItem;
import com.mall.tester.service.TesterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 试用品接口：列表、入库、领用、调柜、冻结、下架、巡查、追溯、统计。
 */
@Path("/api/testers")
@Produces(MediaType.APPLICATION_JSON)
public class TesterResource {

    @Inject
    TesterService service;

    @GET
    public List<Dtos.TesterView> list(@QueryParam("status") String status,
                                      @QueryParam("counter") String counter,
                                      @QueryParam("q") String keyword) {
        return service.listViews(status, counter, keyword);
    }

    @GET
    @Path("/stats")
    public Dtos.StatsView stats() {
        return service.stats();
    }

    @GET
    @Path("/counters")
    public List<String> counters() {
        return service.counters();
    }

    @GET
    @Path("/next-code")
    public Map<String, String> nextCode() {
        return Map.of("itemCode", service.nextItemCode());
    }

    @POST
    @Path("/stock-in")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Dtos.TesterView> stockIn(Dtos.StockInRequest req) {
        return service.stockIn(req).stream()
                .map(t -> service.toView(t, LocalDate.now()))
                .toList();
    }

    @POST
    @Path("/issue")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dtos.TesterView issue(Dtos.IssueRequest req) {
        TesterItem item = service.issue(req);
        return service.toView(item, LocalDate.now());
    }

    @GET
    @Path("/{code}")
    public Dtos.TesterView detail(@PathParam("code") String code) {
        return service.toView(service.findByCode(code), LocalDate.now());
    }

    @GET
    @Path("/{code}/events")
    public List<Dtos.EventView> events(@PathParam("code") String code) {
        return service.eventsOf(code);
    }

    @POST
    @Path("/{code}/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dtos.TesterView transfer(@PathParam("code") String code, Dtos.TransferRequest req) {
        return service.toView(service.transfer(code, req), LocalDate.now());
    }

    @POST
    @Path("/{code}/freeze")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dtos.TesterView freeze(@PathParam("code") String code, Dtos.ReasonRequest req) {
        return service.toView(service.freeze(code, req), LocalDate.now());
    }

    @POST
    @Path("/{code}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Dtos.TesterView remove(@PathParam("code") String code, Dtos.ReasonRequest req) {
        return service.toView(service.remove(code, req), LocalDate.now());
    }

    @POST
    @Path("/{code}/patrol")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> patrol(@PathParam("code") String code, Dtos.PatrolRequest req) {
        var record = service.patrol(code, req);
        return Map.of("id", record.id, "condition", record.condition.name());
    }

    /** 手动触发一次过期巡检（系统也会定时自动执行） */
    @POST
    @Path("/enforce-expiries")
    public Map<String, Integer> enforceExpiries() {
        return Map.of("removed", service.enforceExpiries());
    }
}
