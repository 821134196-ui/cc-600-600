package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 召回通知：由本地模拟接口同步而来。
 */
@Entity
@Table(name = "recall_notice")
public class RecallNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 召回通知编号（外部系统编号，唯一，用于幂等同步） */
    @Column(nullable = false, unique = true)
    public String noticeNo;

    /** 被召回的正装批次号 */
    @Column(nullable = false)
    public String batchNo;

    public String productName;

    @Column(length = 1024)
    public String reason;

    public LocalDate noticeDate;

    /** 来源系统 */
    public String source;

    @Column(nullable = false)
    public Instant syncedAt = Instant.now();
}
