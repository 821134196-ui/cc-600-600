package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 召回通知（由本地模拟接口同步而来） */
@Entity
@Table(name = "recall_notice")
public class RecallNotice extends BaseEntity {

    @Column(nullable = false, unique = true)
    public String noticeCode;

    /** 涉及的正装批次号 */
    @Column(nullable = false)
    public String batchNo;

    public String productName;

    @Column(length = 1024)
    public String reason;

    public LocalDate publishedAt;

    public String source;

    public LocalDateTime syncedAt;
}
