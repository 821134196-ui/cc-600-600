package com.mall.tester.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API 传输对象集合。
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- 请求 ----------

    public record BatchRequest(String batchNo, String productName, String brand,
                               String category, Integer paoMonths, LocalDate productionDate) {
    }

    public record StockInRequest(Long batchId, Integer count, String operator) {
    }

    public record IssueRequest(String itemCode, LocalDate openedDate,
                               String counterLocation, String operator) {
    }

    public record TransferRequest(String counterLocation, String operator) {
    }

    public record ReasonRequest(String reason, String operator) {
    }

    public record PatrolRequest(String condition, String note, String inspector,
                                LocalDate patrolDate) {
    }

    /** 销毁请求：declaredQuantity 为登记的销毁数量，必须与实际提交样品件数一致 */
    public record DestroyRequest(List<String> itemCodes, Integer declaredQuantity,
                                 String reason, String handler) {
    }

    // ---------- 视图 ----------

    /** 试用品列表/详情视图：携带计算出的效期信息 */
    public record TesterView(
            Long id,
            String itemCode,
            String status,
            String counterLocation,
            LocalDate openedDate,
            LocalDate expiryDate,
            Long daysLeft,
            boolean expired,
            boolean nearExpiry,
            boolean recalled,
            int priority,
            String priorityLabel,
            Long batchId,
            String batchNo,
            String productName,
            String brand,
            int paoMonths,
            Instant createdAt) {
    }

    public record StatsView(
            long unopened,          // 未开封备品
            long inUse,             // 已开封在用
            long frozen,            // 冻结
            long pendingDestruction,// 已下架待销毁
            long destroyed,         // 已销毁
            long nearExpiry,        // 临期（在用）
            long expired,           // 已过期（在用，理论上会被自动下架）
            long recalledBatches    // 召回批次数量
    ) {
    }

    public record EventView(Long id, String eventType, String detail,
                            String operator, Instant eventAt) {
    }

    public record DestructionView(Long id, String itemCode, String productName, String batchNo,
                                  String groupNo, int quantity, String reason,
                                  String handler, Instant destroyedAt) {
    }

    /** 销毁核对结果：销毁记录数、记录总数量、已销毁状态样品数三者必须一致 */
    public record ReconcileView(long destroyedItemCount, long destructionRecordCount,
                                long totalDestroyedQuantity, boolean consistent) {
    }

    public record RecallSyncResult(int fetched, int newNotices, int batchesMarked,
                                   int itemsRemoved, int stockFrozen) {
    }

    public record PatrolView(Long id, String itemCode, String productName,
                             LocalDate patrolDate, String condition, String note,
                             String inspector, Instant createdAt) {
    }

    public record PendingDestructionView(String itemCode, String productName,
                                         String batchNo, String status, String counterLocation) {
    }
}
