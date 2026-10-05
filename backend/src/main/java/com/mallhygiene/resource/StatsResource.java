package com.mallhygiene.resource;

import com.mallhygiene.domain.DestructionBatch;
import com.mallhygiene.domain.ItemStatus;
import com.mallhygiene.domain.Product;
import com.mallhygiene.domain.RecallNotice;
import com.mallhygiene.domain.TesterItem;
import com.mallhygiene.dto.Dtos.StatsView;
import com.mallhygiene.service.ExpiryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

/** 汇总统计：未开封备品与已开封试用品分开统计 */
@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {

    @Inject
    ExpiryService expiryService;

    @GET
    public StatsView stats() {
        LocalDate today = LocalDate.now();
        long sealed = TesterItem.count("status = ?1", ItemStatus.SEALED);
        long inUse = TesterItem.count("status = ?1", ItemStatus.IN_USE);
        long frozen = TesterItem.count("status = ?1", ItemStatus.FROZEN);
        long removed = TesterItem.count("status = ?1", ItemStatus.REMOVED);
        long destroyed = TesterItem.count("status = ?1", ItemStatus.DESTROYED);

        List<TesterItem> active = TesterItem.list("status = ?1", ItemStatus.IN_USE);
        long nearExpiry = active.stream()
                .filter(i -> expiryService.isNearExpiry(i.expiryDate, today)).count();
        long expiredInUse = active.stream()
                .filter(i -> expiryService.isExpired(i.expiryDate, today)).count();

        List<DestructionBatch> batches = DestructionBatch.listAll();
        long declared = batches.stream().mapToLong(b -> b.declaredQuantity).sum();
        long actual = batches.stream().mapToLong(b -> b.actualQuantity).sum();

        return new StatsView(sealed, inUse, frozen, removed, destroyed, nearExpiry, expiredInUse,
                Product.count(), RecallNotice.count(), batches.size(), declared, actual);
    }
}
