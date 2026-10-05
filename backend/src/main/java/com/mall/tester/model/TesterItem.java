package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 试用品：每件有独立编号 itemCode。
 * 生命周期：UNOPENED(未开封备品) -> IN_USE(已开封在用) -> REMOVED(已下架) -> DESTROYED(已销毁)
 * 任意环节可 FROZEN(冻结)，冻结后不得再次领用。
 */
@Entity
@Table(name = "tester_item")
public class TesterItem {

    public enum Status {
        UNOPENED,   // 未开封备品
        IN_USE,     // 已开封，柜台在用
        FROZEN,     // 冻结，不得再次领用
        REMOVED,    // 已下架，等待销毁
        DESTROYED   // 已销毁
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 试用品独立编号，全局唯一，全链路追溯的主键 */
    @Column(nullable = false, unique = true)
    public String itemCode;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id")
    public ProductBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.UNOPENED;

    /** 开封日期：效期计算基准。调柜不改变该日期。 */
    public LocalDate openedDate;

    /** 当前所在柜位 */
    public String counterLocation;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
