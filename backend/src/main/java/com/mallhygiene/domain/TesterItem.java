package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 试用品：每件独立编号，按实际开封日期管理使用期限 */
@Entity
@Table(name = "tester_item")
public class TesterItem extends BaseEntity {

    /** 独立编号（全系统唯一，贯穿领用到销毁） */
    @Column(nullable = false, unique = true)
    public String code;

    /** 对应的正装批次 */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ItemStatus status;

    /** 备品入库日期 */
    public LocalDate stockedAt;

    /** 开封日期（领用时登记；调柜不改变） */
    public LocalDate openedDate;

    /** 到期日 = 开封日期 + 开封后使用月数 */
    public LocalDate expiryDate;

    /** 当前柜位 */
    public String counterLocation;

    /** 下架原因：EXPIRED / RECALLED / CONTAMINATED / MANUAL */
    public String removedReason;

    public LocalDateTime removedAt;

    /** 所属销毁批次（销毁后回填，用于逐件核对） */
    @ManyToOne(fetch = FetchType.LAZY)
    public DestructionBatch destructionBatch;

    @Version
    public long version;
}
