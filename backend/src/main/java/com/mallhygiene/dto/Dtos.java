package com.mallhygiene.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** API 请求/响应对象 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- 请求 ----------

    public record ProductReq(String name, String brand, String batchNo, LocalDate productionDate,
                             Integer shelfMonths, Integer paoMonths) {
    }

    public record StockInReq(String code, Long productId, String operator) {
    }

    public record IssueReq(LocalDate openedDate, String counterLocation, String operator) {
    }

    public record TransferReq(String toLocation, String operator) {
    }

    public record ReasonReq(String reason, String operator) {
    }

    public record InspectReq(String result, String inspector, String note) {
    }

    public record DestroyReq(List<Long> itemIds, String reason, String handler) {
    }

    // ---------- 视图 ----------

    /** 列表行：带实时计算的卫生状态与排序优先级 */
    public record ItemView(long id, String code, String productName, String brand, String batchNo,
                           String status, String condition,
                           LocalDate stockedAt, LocalDate openedDate, LocalDate expiryDate,
                           Long daysToExpiry, String counterLocation,
                           String removedReason, LocalDateTime removedAt,
                           LocalDate sealedExpiryDate, Long destructionBatchId, int priority) {
    }

    public record EventView(String type, LocalDateTime eventAt, String operator,
                            String fromLocation, String toLocation, String reason, String note) {
    }

    public record InspectionView(long id, String itemCode, String productName, LocalDateTime inspectedAt,
                                 String inspector, String result, String note) {
    }

    public record DestructionView(String batchCode, int declaredQuantity, int actualQuantity,
                                  String reason, String handler, LocalDateTime destroyedAt) {
    }

    public record TrailView(ItemView item, List<EventView> events, List<InspectionView> inspections,
                            DestructionView destruction) {
    }

    public record StatsView(long sealed, long inUse, long frozen, long removedPending, long destroyed,
                            long nearExpiry, long expiredInUse, long products, long recallNotices,
                            long destructionBatches, long destroyedDeclared, long destroyedActual) {
    }

    // ---------- 召回 ----------

    /** 本地模拟接口返回的召回通知 */
    public record MockRecall(String noticeCode, String batchNo, String productName,
                             String reason, LocalDate publishedAt, String source) {
    }

    public record SyncResult(int noticesReceived, int noticesCreated, int itemsFlagged) {
    }

    // ---------- 销毁核对 ----------

    public record BatchReconcile(String batchCode, int declared, int actual, long linkedDestroyedItems,
                                 boolean ok) {
    }

    public record ReconcileView(List<BatchReconcile> batches, long totalDeclared, long totalActual,
                                long totalDestroyedItems, long pendingRemoval, boolean ok) {
    }
}
