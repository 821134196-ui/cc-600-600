package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 巡查记录：店员巡查时登记污染、临期、到期等情况。
 */
@Entity
@Table(name = "patrol_record", indexes = @Index(name = "idx_patrol_item", columnList = "item_id"))
public class PatrolRecord {

    public enum Condition {
        NORMAL,        // 正常
        CONTAMINATED,  // 污染
        NEAR_EXPIRY,   // 临期
        EXPIRED        // 到期
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id")
    public TesterItem item;

    @Column(nullable = false)
    public LocalDate patrolDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Condition condition;

    @Column(length = 1024)
    public String note;

    /** 巡查人 */
    public String inspector;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
