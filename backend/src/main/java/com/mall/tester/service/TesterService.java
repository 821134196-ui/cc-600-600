package com.mall.tester.service;

import com.mall.tester.dto.Dtos;
import com.mall.tester.dto.Dtos.*;
import com.mall.tester.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 试用品核心业务：领用、调柜、冻结、巡查、下架、销毁、统计、追溯。
 */
@ApplicationScoped
public class TesterService {

    @Inject
    EntityManager em;

    @Inject
    ExpiryService expiryService;

    // ---------- 编号 ----------

    /** 生成下一个试用品独立编号 TST-00001 */
    public String nextItemCode() {
        List<String> codes = em.createQuery(
                        "select t.itemCode from TesterItem t where t.itemCode like 'TST-%'", String.class)
                .getResultList();
        int max = codes.stream()
                .map(c -> c.substring(4))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return String.format("TST-%05d", max + 1);
    }

    // ---------- 批次 ----------

    @Transactional
    public ProductBatch createBatch(Dtos.BatchRequest req) {
        if (req.batchNo() == null || req.batchNo().isBlank()) {
            throw BizException.badRequest("批次号不能为空");
        }
        if (req.productName() == null || req.productName().isBlank()) {
            throw BizException.badRequest("品名不能为空");
        }
        if (req.paoMonths() == null || req.paoMonths() <= 0) {
            throw BizException.badRequest("开封后使用月数必须为正数");
        }
        Long exists = em.createQuery(
                        "select count(b) from ProductBatch b where b.batchNo = :no", Long.class)
                .setParameter("no", req.batchNo()).getSingleResult();
        if (exists > 0) {
            throw BizException.conflict("批次号已存在: " + req.batchNo());
        }
        ProductBatch b = new ProductBatch();
        b.batchNo = req.batchNo().trim();
        b.productName = req.productName().trim();
        b.brand = req.brand();
        b.category = req.category();
        b.paoMonths = req.paoMonths();
        b.productionDate = req.productionDate();
        em.persist(b);
        return b;
    }

    public List<ProductBatch> listBatches() {
        return em.createQuery("select b from ProductBatch b order by b.batchNo", ProductBatch.class)
                .getResultList();
    }

    // ---------- 备品入库 ----------

    @Transactional
    public List<TesterItem> stockIn(Dtos.StockInRequest req) {
        ProductBatch batch = findBatch(req.batchId());
        if (batch.recalled) {
            throw BizException.conflict("批次 " + batch.batchNo + " 已召回，禁止入库");
        }
        int count = req.count() == null ? 1 : req.count();
        if (count <= 0 || count > 500) {
            throw BizException.badRequest("入库数量必须在 1-500 之间");
        }
        List<TesterItem> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TesterItem item = new TesterItem();
            item.itemCode = nextItemCode();
            item.batch = batch;
            item.status = TesterItem.Status.UNOPENED;
            em.persist(item);
            em.persist(new TesterEvent(item, TesterEvent.EventType.STOCK_IN,
                    "备品入库，批次 " + batch.batchNo, req.operator()));
            created.add(item);
        }
        return created;
    }

    // ---------- 领用（开封） ----------

    @Transactional
    public TesterItem issue(Dtos.IssueRequest req) {
        if (req.itemCode() == null || req.itemCode().isBlank()) {
            throw BizException.badRequest("试用品编号不能为空");
        }
        TesterItem item = findByCode(req.itemCode());
        // 冻结的试用品不得再次领用
        if (item.status == TesterItem.Status.FROZEN) {
            throw BizException.conflict("试用品 " + item.itemCode + " 已冻结，不得再次领用");
        }
        if (item.status != TesterItem.Status.UNOPENED) {
            throw BizException.conflict("试用品 " + item.itemCode + " 当前状态为 "
                    + statusLabel(item.status) + "，只有未开封备品可以领用");
        }
        if (item.batch.recalled) {
            throw BizException.conflict("批次 " + item.batch.batchNo + " 已召回，禁止领用");
        }
        if (req.openedDate() == null) {
            throw BizException.badRequest("开封日期不能为空");
        }
        if (req.openedDate().isAfter(LocalDate.now())) {
            throw BizException.badRequest("开封日期不能晚于今天");
        }
        if (req.counterLocation() == null || req.counterLocation().isBlank()) {
            throw BizException.badRequest("柜位不能为空");
        }
        item.openedDate = req.openedDate();
        item.counterLocation = req.counterLocation().trim();
        item.status = TesterItem.Status.IN_USE;
        em.persist(new TesterEvent(item, TesterEvent.EventType.ISSUED,
                "领用开封，柜位 " + item.counterLocation + "，开封日期 " + item.openedDate
                        + "，效期至 " + expiryService.expiryDateOf(item),
                req.operator()));
        return item;
    }

    // ---------- 调柜 ----------

    /**
     * 调柜：仅变更柜位。开封日期与效期保持不变（openedDate 字段不做任何修改）。
     */
    @Transactional
    public TesterItem transfer(String code, Dtos.TransferRequest req) {
        TesterItem item = findByCode(code);
        if (item.status != TesterItem.Status.IN_USE) {
            throw BizException.conflict("只有在用试用品可以调柜，当前状态: " + statusLabel(item.status));
        }
        if (req.counterLocation() == null || req.counterLocation().isBlank()) {
            throw BizException.badRequest("新柜位不能为空");
        }
        String from = item.counterLocation;
        item.counterLocation = req.counterLocation().trim();
        // 注意：openedDate 不被动，效期自然沿用
        em.persist(new TesterEvent(item, TesterEvent.EventType.TRANSFERRED,
                "调柜 " + from + " -> " + item.counterLocation
                        + "（开封日期 " + item.openedDate + " 不变，效期仍至 "
                        + expiryService.expiryDateOf(item) + "）",
                req.operator()));
        return item;
    }

    // ---------- 冻结 ----------

    @Transactional
    public TesterItem freeze(String code, Dtos.ReasonRequest req) {
        TesterItem item = findByCode(code);
        if (item.status == TesterItem.Status.DESTROYED) {
            throw BizException.conflict("试用品已销毁，无法冻结");
        }
        if (item.status == TesterItem.Status.FROZEN) {
            throw BizException.conflict("试用品已处于冻结状态");
        }
        item.status = TesterItem.Status.FROZEN;
        em.persist(new TesterEvent(item, TesterEvent.EventType.FROZEN,
                "冻结，原因: " + nullToDash(req.reason()) + "。冻结期间不得再次领用。",
                req.operator()));
        return item;
    }

    // ---------- 手动下架 ----------

    @Transactional
    public TesterItem remove(String code, Dtos.ReasonRequest req) {
        TesterItem item = findByCode(code);
        if (item.status != TesterItem.Status.IN_USE && item.status != TesterItem.Status.FROZEN) {
            throw BizException.conflict("当前状态 " + statusLabel(item.status) + " 不能下架");
        }
        item.status = TesterItem.Status.REMOVED;
        em.persist(new TesterEvent(item, TesterEvent.EventType.REMOVED,
                "手动下架，原因: " + nullToDash(req.reason()), req.operator()));
        return item;
    }

    // ---------- 巡查 ----------

    @Transactional
    public PatrolRecord patrol(String code, Dtos.PatrolRequest req) {
        TesterItem item = findByCode(code);
        if (item.status != TesterItem.Status.IN_USE) {
            throw BizException.conflict("只有在用试用品可登记巡查，当前状态: " + statusLabel(item.status));
        }
        PatrolRecord.Condition condition;
        try {
            condition = PatrolRecord.Condition.valueOf(req.condition());
        } catch (Exception e) {
            throw BizException.badRequest("巡查结论无效: " + req.condition()
                    + "，可选 NORMAL/CONTAMINATED/NEAR_EXPIRY/EXPIRED");
        }
        PatrolRecord record = new PatrolRecord();
        record.item = item;
        record.patrolDate = req.patrolDate() != null ? req.patrolDate() : LocalDate.now();
        record.condition = condition;
        record.note = req.note();
        record.inspector = req.inspector();
        em.persist(record);
        em.persist(new TesterEvent(item, TesterEvent.EventType.PATROL,
                "巡查结论 " + conditionLabel(condition) + (req.note() != null ? "，备注: " + req.note() : ""),
                req.inspector()));
        // 污染或到期 -> 立即下架，进入待销毁
        if (condition == PatrolRecord.Condition.CONTAMINATED) {
            item.status = TesterItem.Status.REMOVED;
            em.persist(new TesterEvent(item, TesterEvent.EventType.REMOVED,
                    "巡查发现污染，立即下架", req.inspector()));
        } else if (condition == PatrolRecord.Condition.EXPIRED) {
            item.status = TesterItem.Status.REMOVED;
            em.persist(new TesterEvent(item, TesterEvent.EventType.EXPIRED_REMOVED,
                    "巡查确认到期，立即下架", req.inspector()));
        }
        return record;
    }

    // ---------- 销毁 ----------

    /**
     * 销毁：登记数量必须与实际提交的样品件数一致；
     * 每件样品必须处于"在库待销毁"状态（已下架或冻结）；
     * 每件样品仅允许销毁一次（业务校验 + item_id 唯一约束双保险）。
     */
    @Transactional
    public String destroy(Dtos.DestroyRequest req) {
        if (req.itemCodes() == null || req.itemCodes().isEmpty()) {
            throw BizException.badRequest("销毁清单不能为空");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw BizException.badRequest("销毁原因不能为空");
        }
        if (req.handler() == null || req.handler().isBlank()) {
            throw BizException.badRequest("经手人不能为空");
        }
        // 数量核对：登记数量 vs 实际样品件数
        int actual = req.itemCodes().size();
        if (req.declaredQuantity() == null || req.declaredQuantity() != actual) {
            throw BizException.conflict("销毁数量核对失败：登记数量 "
                    + req.declaredQuantity() + " 与实际在库样品 " + actual + " 件不一致");
        }
        // 去重，防止同一编号在清单中重复出现
        Set<String> unique = new LinkedHashSet<>(req.itemCodes());
        if (unique.size() != actual) {
            throw BizException.badRequest("销毁清单中存在重复编号");
        }
        String groupNo = "DST-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        for (String code : unique) {
            TesterItem item = findByCode(code.trim());
            if (item.status == TesterItem.Status.DESTROYED) {
                throw BizException.conflict("试用品 " + item.itemCode + " 已销毁，禁止重复处理");
            }
            if (item.status != TesterItem.Status.REMOVED && item.status != TesterItem.Status.FROZEN) {
                throw BizException.conflict("试用品 " + item.itemCode + " 状态为 "
                        + statusLabel(item.status) + "，须先下架方可销毁");
            }
            Long already = em.createQuery(
                            "select count(d) from DestructionRecord d where d.item.id = :id", Long.class)
                    .setParameter("id", item.id).getSingleResult();
            if (already > 0) {
                throw BizException.conflict("试用品 " + item.itemCode + " 已存在销毁记录，禁止重复处理");
            }
        }
        for (String code : unique) {
            TesterItem item = findByCode(code.trim());
            DestructionRecord record = new DestructionRecord();
            record.item = item;
            record.groupNo = groupNo;
            record.quantity = 1;
            record.reason = req.reason().trim();
            record.handler = req.handler().trim();
            em.persist(record);
            item.status = TesterItem.Status.DESTROYED;
            em.persist(new TesterEvent(item, TesterEvent.EventType.DESTROYED,
                    "销毁（批次 " + groupNo + "），原因: " + req.reason(), req.handler()));
        }
        return groupNo;
    }

    /** 销毁核对：已销毁状态样品数 == 销毁记录条数 == 记录数量合计 */
    public ReconcileView reconcile() {
        long destroyedItems = countByStatus(TesterItem.Status.DESTROYED);
        long recordCount = em.createQuery("select count(d) from DestructionRecord d", Long.class)
                .getSingleResult();
        Long qtySum = em.createQuery("select coalesce(sum(d.quantity),0) from DestructionRecord d", Long.class)
                .getSingleResult();
        boolean consistent = destroyedItems == recordCount && recordCount == qtySum;
        return new ReconcileView(destroyedItems, recordCount, qtySum, consistent);
    }

    // ---------- 过期/召回自动下架 ----------

    /** 扫描在用试用品，过期立即下架。返回下架件数。 */
    @Transactional
    public int enforceExpiries() {
        LocalDate today = LocalDate.now();
        List<TesterItem> inUse = em.createQuery(
                        "select t from TesterItem t join fetch t.batch where t.status = :s", TesterItem.class)
                .setParameter("s", TesterItem.Status.IN_USE)
                .getResultList();
        AtomicInteger removed = new AtomicInteger();
        for (TesterItem item : inUse) {
            if (expiryService.isExpired(item.openedDate, item.batch.paoMonths, today)) {
                item.status = TesterItem.Status.REMOVED;
                em.persist(new TesterEvent(item, TesterEvent.EventType.EXPIRED_REMOVED,
                        "超过卫生效期 " + expiryService.expiryDateOf(item) + "，自动下架", "system"));
                removed.incrementAndGet();
            }
        }
        return removed.get();
    }

    /** 召回生效：标记批次，召回批次下的在用样品立即下架、未开封备品冻结。 */
    @Transactional
    public int[] applyRecall(ProductBatch batch, String reason, LocalDate recallDate) {
        batch.recalled = true;
        batch.recallReason = reason;
        batch.recallDate = recallDate;
        List<TesterItem> items = em.createQuery(
                        "select t from TesterItem t where t.batch.id = :bid and t.status in (:s1,:s2)",
                        TesterItem.class)
                .setParameter("bid", batch.id)
                .setParameter("s1", TesterItem.Status.IN_USE)
                .setParameter("s2", TesterItem.Status.UNOPENED)
                .getResultList();
        int removed = 0, frozen = 0;
        for (TesterItem item : items) {
            if (item.status == TesterItem.Status.IN_USE) {
                item.status = TesterItem.Status.REMOVED;
                em.persist(new TesterEvent(item, TesterEvent.EventType.RECALL_REMOVED,
                        "批次召回（" + reason + "），立即下架", "system"));
                removed++;
            } else {
                item.status = TesterItem.Status.FROZEN;
                em.persist(new TesterEvent(item, TesterEvent.EventType.FROZEN,
                        "批次召回（" + reason + "），备品冻结，不得领用", "system"));
                frozen++;
            }
        }
        return new int[]{removed, frozen};
    }

    // ---------- 查询 ----------

    public TesterItem findByCode(String code) {
        List<TesterItem> list = em.createQuery(
                        "select t from TesterItem t join fetch t.batch where t.itemCode = :c",
                        TesterItem.class)
                .setParameter("c", code).getResultList();
        if (list.isEmpty()) {
            throw BizException.notFound("试用品不存在: " + code);
        }
        return list.get(0);
    }

    private ProductBatch findBatch(Long id) {
        ProductBatch b = em.find(ProductBatch.class, id);
        if (b == null) {
            throw BizException.notFound("批次不存在: " + id);
        }
        return b;
    }

    /**
     * 列表：临期及待处理实物优先。
     * 优先级：0 待处理（已下架待销毁/在用但已过期/召回批次在用） >
     *        1 临期在用 > 2 冻结 > 3 正常在用 > 4 未开封备品 > 5 已销毁
     */
    public List<TesterView> listViews(String statusFilter, String counterFilter, String keyword) {
        List<TesterItem> items = em.createQuery(
                        "select t from TesterItem t join fetch t.batch", TesterItem.class)
                .getResultList();
        LocalDate today = LocalDate.now();
        return items.stream()
                .map(t -> toView(t, today))
                .filter(v -> statusFilter == null || statusFilter.isBlank()
                        || v.status().equals(statusFilter))
                .filter(v -> counterFilter == null || counterFilter.isBlank()
                        || counterFilter.equals(v.counterLocation()))
                .filter(v -> keyword == null || keyword.isBlank()
                        || v.itemCode().contains(keyword)
                        || (v.productName() != null && v.productName().contains(keyword))
                        || (v.batchNo() != null && v.batchNo().contains(keyword)))
                .sorted(Comparator.comparingInt(TesterView::priority)
                        .thenComparing(v -> v.daysLeft() == null ? Long.MAX_VALUE : v.daysLeft())
                        .thenComparing(TesterView::itemCode))
                .toList();
    }

    public TesterView toView(TesterItem item, LocalDate today) {
        Integer pao = item.batch != null ? item.batch.paoMonths : 0;
        LocalDate expiry = expiryService.expiryDate(item.openedDate, pao);
        Long daysLeft = expiry == null ? null : expiryService.daysLeft(item.openedDate, pao, today);
        boolean expired = expiry != null && expiryService.isExpired(item.openedDate, pao, today);
        boolean nearExpiry = expiry != null && expiryService.isNearExpiry(item.openedDate, pao, today);
        boolean recalled = item.batch != null && item.batch.recalled;
        int priority;
        String label;
        switch (item.status) {
            case REMOVED -> { priority = 0; label = "待销毁"; }
            case IN_USE -> {
                if (expired || recalled) { priority = 0; label = expired ? "已过期-待处理" : "召回-待处理"; }
                else if (nearExpiry) { priority = 1; label = "临期"; }
                else { priority = 3; label = "正常"; }
            }
            case FROZEN -> { priority = 2; label = "冻结"; }
            case UNOPENED -> { priority = 4; label = "未开封备品"; }
            case DESTROYED -> { priority = 5; label = "已销毁"; }
            default -> { priority = 9; label = "-"; }
        }
        return new TesterView(
                item.id, item.itemCode, item.status.name(), item.counterLocation,
                item.openedDate, expiry, daysLeft, expired, nearExpiry, recalled,
                priority, label,
                item.batch != null ? item.batch.id : null,
                item.batch != null ? item.batch.batchNo : null,
                item.batch != null ? item.batch.productName : null,
                item.batch != null ? item.batch.brand : null,
                pao, item.createdAt);
    }

    public StatsView stats() {
        LocalDate today = LocalDate.now();
        long unopened = countByStatus(TesterItem.Status.UNOPENED);
        long inUse = countByStatus(TesterItem.Status.IN_USE);
        long frozen = countByStatus(TesterItem.Status.FROZEN);
        long removed = countByStatus(TesterItem.Status.REMOVED);
        long destroyed = countByStatus(TesterItem.Status.DESTROYED);
        List<TesterItem> inUseItems = em.createQuery(
                        "select t from TesterItem t join fetch t.batch where t.status = :s", TesterItem.class)
                .setParameter("s", TesterItem.Status.IN_USE).getResultList();
        long near = inUseItems.stream()
                .filter(t -> expiryService.isNearExpiry(t.openedDate, t.batch.paoMonths, today)).count();
        long expired = inUseItems.stream()
                .filter(t -> expiryService.isExpired(t.openedDate, t.batch.paoMonths, today)).count();
        long recalledBatches = em.createQuery(
                "select count(b) from ProductBatch b where b.recalled = true", Long.class).getSingleResult();
        return new StatsView(unopened, inUse, frozen, removed, destroyed, near, expired, recalledBatches);
    }

    public List<EventView> eventsOf(String code) {
        TesterItem item = findByCode(code);
        List<TesterEvent> events = em.createQuery(
                        "select e from TesterEvent e where e.item.id = :id order by e.eventAt asc, e.id asc",
                        TesterEvent.class)
                .setParameter("id", item.id).getResultList();
        return events.stream()
                .map(e -> new EventView(e.id, e.eventType.name(), e.detail, e.operator, e.eventAt))
                .toList();
    }

    public List<PatrolView> recentPatrols(int limit) {
        List<PatrolRecord> records = em.createQuery(
                        "select p from PatrolRecord p join fetch p.item i join fetch i.batch order by p.createdAt desc",
                        PatrolRecord.class)
                .setMaxResults(limit).getResultList();
        return records.stream().map(p -> new PatrolView(
                p.id, p.item.itemCode, p.item.batch.productName, p.patrolDate,
                p.condition.name(), p.note, p.inspector, p.createdAt)).toList();
    }

    public List<DestructionView> destructions() {
        List<DestructionRecord> records = em.createQuery(
                        "select d from DestructionRecord d join fetch d.item i join fetch i.batch order by d.destroyedAt desc",
                        DestructionRecord.class)
                .getResultList();
        return records.stream().map(d -> new DestructionView(
                d.id, d.item.itemCode, d.item.batch.productName, d.item.batch.batchNo,
                d.groupNo, d.quantity, d.reason, d.handler, d.destroyedAt)).toList();
    }

    /** 待销毁实物清单（已下架或冻结、尚未销毁） */
    public List<PendingDestructionView> pendingDestruction() {
        List<TesterItem> items = em.createQuery(
                        "select t from TesterItem t join fetch t.batch where t.status in (:s1,:s2) order by t.itemCode",
                        TesterItem.class)
                .setParameter("s1", TesterItem.Status.REMOVED)
                .setParameter("s2", TesterItem.Status.FROZEN)
                .getResultList();
        return items.stream().map(t -> new PendingDestructionView(
                t.itemCode, t.batch.productName, t.batch.batchNo,
                t.status.name(), t.counterLocation)).toList();
    }

    public List<String> counters() {
        return em.createQuery(
                        "select distinct t.counterLocation from TesterItem t where t.counterLocation is not null order by t.counterLocation",
                        String.class)
                .getResultList();
    }

    private long countByStatus(TesterItem.Status status) {
        return em.createQuery("select count(t) from TesterItem t where t.status = :s", Long.class)
                .setParameter("s", status).getSingleResult();
    }

    public static String statusLabel(TesterItem.Status s) {
        return switch (s) {
            case UNOPENED -> "未开封";
            case IN_USE -> "在用";
            case FROZEN -> "冻结";
            case REMOVED -> "已下架";
            case DESTROYED -> "已销毁";
        };
    }

    public static String conditionLabel(PatrolRecord.Condition c) {
        return switch (c) {
            case NORMAL -> "正常";
            case CONTAMINATED -> "污染";
            case NEAR_EXPIRY -> "临期";
            case EXPIRED -> "到期";
        };
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
