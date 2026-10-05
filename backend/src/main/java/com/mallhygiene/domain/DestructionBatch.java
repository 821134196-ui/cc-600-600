package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 销毁批次：登记数量、原因、经手人；数量须与实际在库逐件核对 */
@Entity
@Table(name = "destruction_batch")
public class DestructionBatch extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String batchCode;

    /** 申报销毁数量 */
    @Column(nullable = false)
    public int declaredQuantity;

    /** 实际处理数量（成功由“已下架”转为“已销毁”的件数） */
    @Column(nullable = false)
    public int actualQuantity;

    @Column(nullable = false)
    public String reason;

    /** 经手人 */
    @Column(nullable = false)
    public String handler;

    @Column(nullable = false)
    public LocalDateTime destroyedAt;
}
