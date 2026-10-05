package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 试用品事件：从入库、领用、调柜到销毁的全链路追溯记录。
 */
@Entity
@Table(name = "tester_event", indexes = {
        @Index(name = "idx_event_item", columnList = "item_id"),
        @Index(name = "idx_event_at", columnList = "eventAt")
})
public class TesterEvent {

    public enum EventType {
        STOCK_IN,        // 备品入库
        ISSUED,          // 领用开封
        TRANSFERRED,     // 调柜（仅变更柜位，开封日期不变）
        FROZEN,          // 冻结
        PATROL,          // 巡查记录
        REMOVED,         // 手动下架
        EXPIRED_REMOVED, // 过期自动下架
        RECALL_REMOVED,  // 召回自动下架
        DESTROYED        // 销毁
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public TesterItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EventType eventType;

    /** 事件说明，如 "柜位 A-01 -> B-03" */
    @Column(length = 1024)
    public String detail;

    /** 经手人 */
    public String operator;

    @Column(nullable = false)
    public Instant eventAt = Instant.now();

    public TesterEvent() {
    }

    public TesterEvent(TesterItem item, EventType type, String detail, String operator) {
        this.item = item;
        this.eventType = type;
        this.detail = detail;
        this.operator = operator;
    }
}
