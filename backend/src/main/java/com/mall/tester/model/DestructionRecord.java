package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 销毁记录：每件试用品最多一条销毁记录（item 唯一约束），
 * 从数据库层面杜绝同一件样品被重复销毁。
 * 一次销毁操作可能包含多件样品，通过 groupNo 关联。
 */
@Entity
@Table(name = "destruction_record", indexes = {
        @Index(name = "idx_destruction_group", columnList = "groupNo")
})
public class DestructionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 被销毁的试用品，唯一 —— 一件样品只能销毁一次 */
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", unique = true)
    public TesterItem item;

    /** 本次销毁操作的批次号（一次操作可销毁多件） */
    @Column(nullable = false)
    public String groupNo;

    /** 销毁数量（按件计，每件样品恒为 1，汇总核对使用） */
    @Column(nullable = false)
    public int quantity = 1;

    @Column(nullable = false, length = 512)
    public String reason;

    /** 经手人 */
    @Column(nullable = false)
    public String handler;

    @Column(nullable = false)
    public Instant destroyedAt = Instant.now();
}
