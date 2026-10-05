package com.mallhygiene.service;

import com.mallhygiene.domain.DestructionBatch;
import com.mallhygiene.domain.EventType;
import com.mallhygiene.domain.InspectionRecord;
import com.mallhygiene.domain.InspectionResult;
import com.mallhygiene.domain.ItemEvent;
import com.mallhygiene.domain.ItemStatus;
import com.mallhygiene.domain.Product;
import com.mallhygiene.domain.TesterItem;
import com.mallhygiene.dto.Dtos.BatchReconcile;
import com.mallhygiene.dto.Dtos.DestructionView;
import com.mallhygiene.dto.Dtos.EventView;
import com.mallhygiene.dto.Dtos.InspectionView;
import com.mallhygiene.dto.Dtos.ItemView;
import com.mallhygiene.dto.Dtos.ReconcileView;
import com.mallhygiene.dto.Dtos.TrailView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 试用品核心业务：领用、调柜、冻结、巡查、下架、销毁、追溯 */
@ApplicationScoped
public class TesterService {

    @Inject
    ExpiryService expiryService;

    // ---------- 查询 ----------

    public TesterItem mustFind(String code) {
        TesterItem item = TesterItem.find("code", code).firstResult();
        if (item == null) {
            throw new NotFoundException("试用品不存在: " + code);
        }
        return item;
    }

    /** 列表：临期及待处理实物排在最前 */
    public List<ItemView> listItems(String status, String counter, String q) {
        StringBuilder jpql = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();
        if (status != null && !status.isBlank()) {
            jpql.append(" and status = :status");
            params.put("status", ItemStatus.valueOf(status));
        }
        if (counter != null && !counter.isBlank()) {
            jpql.append(" and counterLocation = :counter");
            params.put("counter", counter);
        }
        if (q != null && !q.isBlank()) {
            jpql.append(" and (code like :q or product.name like :q or product.batchNo like :q)");
            params.put("q", "%" + q + "%");
        }
        List<TesterItem> items = TesterItem.find(jpql.toString(), params).list();
        LocalDate today = LocalDate.now();
        return items.stream()
                .map(i -> toView(i, today))
                .sorted(Comparator.comparingInt(ItemView::priority)
                        .thenComparing(ItemView::expiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ItemView::code))
                .toList();
    }

    public ItemView toView(TesterItem item, LocalDate today) {
        String condition = expiryService.conditionOf(item, today);
        Long daysToExpiry = item.expiryDate == null ? null : ChronoUnit.DAYS.between(today, item.expiryDate);
        Long batchId = item.destructionBatch == null ? null : item.destructionBatch.id;
        return new ItemView(item.id, item.code, item.product.name, item.product.brand, item.product.batchNo,
                item.status.name(), condition, item.stockedAt, item.openedDate, item.expiryDate, daysToExpiry,
                item.counterLocation, item.removedReason, item.removedAt,
                expiryService.sealedExpiry(item.product), batchId, priorityOf(item.status, condition));
    }

    /** 排序优先级：待销毁 > 已过期 > 临期 > 冻结 > 正常在用 > 备品 > 已销毁 */
    static int priorityOf(ItemStatus status, String condition) {
        return switch (status) {
            case REMOVED -> 0;
            case IN_USE -> "EXPIRED".equals(condition) ? 1 : "NEAR_EXPIRY".equals(condition) ? 2 : 4;
            case FROZEN -> 3;
            case SEALED -> 5;
            case DESTROYED -> 6;
        };
    }

    /** 履历：沿编号查询从领用到销毁的全部记录 */
    public TrailView trail(String code) {
        TesterItem item = mustFind(code);
        List<EventView> events = ItemEvent.<ItemEvent>find("item = ?1 order by eventAt asc, id asc", item)
                .list().stream()
                .map(e -> new EventView(e.type.name(), e.eventAt, e.operator, e.fromLocation, e.toLocation,
                        e.reason, e.note))
                .toList();
        List<InspectionView> inspections = InspectionRecord
                .<InspectionRecord>find("item = ?1 order by inspectedAt asc, id asc", item)
                .list().stream()
                .map(r -> new InspectionView(r.id, item.code, item.product.name, r.inspectedAt, r.inspector,
                        r.result.name(), r.note))
                .toList();
        DestructionView destruction = null;
        if (item.destructionBatch != null) {
            DestructionBatch b = item.destructionBatch;
            destruction = new DestructionView(b.batchCode, b.declaredQuantity, b.actualQuantity, b.reason,
                    b.handler, b.destroyedAt);
        }
        return new TrailView(toView(item, LocalDate.now()), events, inspections, destruction);
    }

    // ---------- 入库 / 领用 ----------

    @Transactional
    public ItemView stockIn(String code, Long productId, String operator) {
        if (productId == null) {
            throw new BadRequestException("必须选择正装批次");
        }
        Product product = Product.findById(productId);
        if (product == null) {
            throw new NotFoundException("商品批次不存在: " + productId);
        }
        if (code == null || code.isBlank()) {
            code = nextItemCode();
        }
        if (TesterItem.count("code", code) > 0) {
            throw new ConflictException("编号已存在: " + code);
        }
        TesterItem item = new TesterItem();
        item.code = code;
        item.product = product;
        item.status = ItemStatus.SEALED;
        item.stockedAt = LocalDate.now();
        item.persist();
        record(item, EventType.STOCK_IN, operator, null, null, null, "备品入库");
        return toView(item, LocalDate.now());
    }

    /** 领用：登记开封日期与柜位，计算到期日 */
    @Transactional
    public ItemView issue(String code, LocalDate openedDate, String counter, String operator) {
        TesterItem item = mustFind(code);
        if (item.status == ItemStatus.FROZEN) {
            throw new ConflictException("试用品已冻结，不得再次领用: " + code);
        }
        if (item.status != ItemStatus.SEALED) {
            throw new ConflictException("仅未开封备品可领用，当前状态: " + item.status);
        }
        if (counter == null || counter.isBlank()) {
            throw new BadRequestException("柜位不能为空");
        }
        LocalDate opened = openedDate != null ? openedDate : LocalDate.now();
        item.openedDate = opened;
        item.expiryDate = expiryService.computeExpiry(opened, item.product.paoMonths);
        item.counterLocation = counter;
        item.status = ItemStatus.IN_USE;
        record(item, EventType.ISSUE, operator, null, counter, null,
                "领用开封，开封日期 " + opened + "，到期日 " + item.expiryDate);
        return toView(item, LocalDate.now());
    }

    // ---------- 调柜 ----------

    /** 调柜：仅改变柜位，原开封时间与到期日继续沿用 */
    @Transactional
    public ItemView transfer(String code, String toLocation, String operator) {
        TesterItem item = mustFind(code);
        if (item.status != ItemStatus.IN_USE && item.status != ItemStatus.SEALED) {
            throw new ConflictException("当前状态不允许调柜: " + item.status);
        }
        if (toLocation == null || toLocation.isBlank()) {
            throw new BadRequestException("目标柜位不能为空");
        }
        String from = item.counterLocation;
        // 注意：这里只写柜位，绝不触碰 openedDate / expiryDate
        item.counterLocation = toLocation;
        record(item, EventType.TRANSFER, operator, from, toLocation, null, "调柜，开封日期与效期不变");
        return toView(item, LocalDate.now());
    }

    // ---------- 冻结 / 解冻 ----------

    @Transactional
    public ItemView freeze(String code, String reason, String operator) {
        TesterItem item = mustFind(code);
        if (item.status != ItemStatus.SEALED && item.status != ItemStatus.IN_USE) {
            throw new ConflictException("仅备品或在用试用品可冻结，当前状态: " + item.status);
        }
        item.status = ItemStatus.FROZEN;
        record(item, EventType.FREEZE, operator, null, null, reason, "冻结，冻结期间不得领用");
        return toView(item, LocalDate.now());
    }

    @Transactional
    public ItemView unfreeze(String code, String operator) {
        TesterItem item = mustFind(code);
        if (item.status != ItemStatus.FROZEN) {
            throw new ConflictException("试用品未处于冻结状态: " + code);
        }
        // 解冻后回到冻结前的状态：未开过封回备品，开过封回在用
        item.status = item.openedDate == null ? ItemStatus.SEALED : ItemStatus.IN_USE;
        record(item, EventType.UNFREEZE, operator, null, null, null, "解冻");
        return toView(item, LocalDate.now());
    }

    // ---------- 下架 ----------

    @Transactional
    public ItemView remove(String code, String reason, String operator) {
        TesterItem item = mustFind(code);
        if (item.status != ItemStatus.IN_USE && item.status != ItemStatus.SEALED
                && item.status != ItemStatus.FROZEN) {
            throw new ConflictException("当前状态不允许下架: " + item.status);
        }
        doRemove(item, reason == null || reason.isBlank() ? "MANUAL" : reason, operator, "手动下架");
        return toView(item, LocalDate.now());
    }

    void doRemove(TesterItem item, String reason, String operator, String note) {
        item.status = ItemStatus.REMOVED;
        item.removedReason = reason;
        item.removedAt = LocalDateTime.now();
        record(item, EventType.REMOVE, operator, item.counterLocation, null, reason, note);
    }

    // ---------- 巡查 ----------

    @Transactional
    public InspectionView inspect(String code, InspectionResult result, String inspector, String note) {
        TesterItem item = mustFind(code);
        if (item.status != ItemStatus.IN_USE) {
            throw new ConflictException("仅在用试用品可巡查登记，当前状态: " + item.status);
        }
        if (inspector == null || inspector.isBlank()) {
            throw new BadRequestException("巡查人不能为空");
        }
        InspectionRecord r = new InspectionRecord();
        r.item = item;
        r.inspectedAt = LocalDateTime.now();
        r.inspector = inspector;
        r.result = result;
        r.note = note;
        r.persist();
        record(item, EventType.INSPECT, inspector, null, null, result.name(), note);
        // 污染或到期：立即下架
        if (result == InspectionResult.CONTAMINATED || result == InspectionResult.EXPIRED) {
            doRemove(item, result.name(), inspector, "巡查发现" + (result == InspectionResult.CONTAMINATED ? "污染" : "到期") + "，立即下架");
        }
        return new InspectionView(r.id, item.code, item.product.name, r.inspectedAt, r.inspector,
                r.result.name(), r.note);
    }

    // ---------- 效期扫描（定时任务 + 手动触发共用） ----------

    /** 把已过到期日的在用试用品自动下架，返回处理件数 */
    @Transactional
    public int removeExpiredItems() {
        LocalDate today = LocalDate.now();
        List<TesterItem> expired = TesterItem.find(
                "status = ?1 and expiryDate is not null and expiryDate < ?2",
                ItemStatus.IN_USE, today).list();
        for (TesterItem item : expired) {
            doRemove(item, "EXPIRED", "system", "效期扫描自动下架，到期日 " + item.expiryDate);
        }
        return expired.size();
    }

    // ---------- 销毁 ----------

    /**
     * 销毁：登记数量、原因、经手人。
     * 每件样品通过“仅当状态仍为 REMOVED 才更新”的守卫式状态迁移转为 DESTROYED，
     * 同一件样品不可能被处理两次；申报数量与实际迁移件数逐件核对。
     */
    @Transactional
    public DestructionBatch destroy(List<Long> itemIds, String reason, String handler) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BadRequestException("销毁清单不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("销毁原因不能为空");
        }
        if (handler == null || handler.isBlank()) {
            throw new BadRequestException("经手人不能为空");
        }
        List<Long> distinctIds = itemIds.stream().distinct().toList();

        DestructionBatch batch = new DestructionBatch();
        batch.batchCode = nextBatchCode();
        batch.declaredQuantity = distinctIds.size();
        batch.actualQuantity = 0;
        batch.reason = reason;
        batch.handler = handler;
        batch.destroyedAt = LocalDateTime.now();
        batch.persist();

        int affected = 0;
        for (Long id : distinctIds) {
            TesterItem item = TesterItem.findById(id);
            if (item == null) {
                throw new NotFoundException("试用品不存在 id=" + id);
            }
            if (item.status != ItemStatus.REMOVED) {
                throw new ConflictException(
                        "试用品 " + item.code + " 状态为 " + item.status + "，仅已下架试用品可销毁");
            }
            // 守卫式迁移：只有仍处于 REMOVED 的行才会被更新，防止重复处理
            int n = TesterItem.update(
                    "status = ?1, destructionBatch = ?2 where id = ?3 and status = ?4",
                    ItemStatus.DESTROYED, batch, id, ItemStatus.REMOVED);
            if (n != 1) {
                throw new ConflictException("试用品 " + item.code + " 已被处理，禁止重复销毁");
            }
            affected++;
            record(item, EventType.DESTROY, handler, item.counterLocation, null, reason,
                    "销毁批次 " + batch.batchCode);
        }
        batch.actualQuantity = affected;
        return batch;
    }

    /** 销毁核对：每批申报数 = 实际处理数 = 库内关联的已销毁件数 */
    public ReconcileView reconcile() {
        List<DestructionBatch> batches = DestructionBatch.listAll();
        List<BatchReconcile> rows = new ArrayList<>();
        for (DestructionBatch b : batches) {
            long linked = TesterItem.count("destructionBatch = ?1 and status = ?2", b, ItemStatus.DESTROYED);
            rows.add(new BatchReconcile(b.batchCode, b.declaredQuantity, b.actualQuantity, linked,
                    b.declaredQuantity == b.actualQuantity && b.actualQuantity == linked));
        }
        long totalDeclared = rows.stream().mapToLong(BatchReconcile::declared).sum();
        long totalActual = rows.stream().mapToLong(BatchReconcile::actual).sum();
        long totalDestroyed = TesterItem.count("status = ?1", ItemStatus.DESTROYED);
        long pending = TesterItem.count("status = ?1", ItemStatus.REMOVED);
        boolean ok = rows.stream().allMatch(BatchReconcile::ok) && totalActual == totalDestroyed;
        return new ReconcileView(rows, totalDeclared, totalActual, totalDestroyed, pending, ok);
    }

    // ---------- 内部工具 ----------

    public void record(TesterItem item, EventType type, String operator, String from, String to,
                       String reason, String note) {
        ItemEvent e = new ItemEvent();
        e.item = item;
        e.type = type;
        e.eventAt = LocalDateTime.now();
        e.operator = operator;
        e.fromLocation = from;
        e.toLocation = to;
        e.reason = reason;
        e.note = note;
        e.persist();
    }

    private String nextItemCode() {
        long n = TesterItem.count() + 1;
        String code;
        do {
            code = "T-" + String.format("%05d", n++);
        } while (TesterItem.count("code", code) > 0);
        return code;
    }

    private String nextBatchCode() {
        long n = DestructionBatch.count() + 1;
        String code;
        do {
            code = "D-" + String.format("%05d", n++);
        } while (DestructionBatch.count("batchCode", code) > 0);
        return code;
    }
}
