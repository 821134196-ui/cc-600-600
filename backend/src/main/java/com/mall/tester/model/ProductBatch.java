package com.mall.tester.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 正装批次：试用品对应的商品批次。
 * paoMonths 为开封后允许使用的月数（PAO），是卫生效期计算的基准。
 */
@Entity
@Table(name = "product_batch")
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 正装批次号，全局唯一 */
    @Column(nullable = false, unique = true)
    public String batchNo;

    @Column(nullable = false)
    public String productName;

    public String brand;

    /** 品类：护肤 / 彩妆 / 香水 等 */
    public String category;

    /** 开封后使用月数（PAO） */
    @Column(nullable = false)
    public int paoMonths;

    public LocalDate productionDate;

    /** 是否被召回 */
    @Column(nullable = false)
    public boolean recalled = false;

    public String recallReason;

    public LocalDate recallDate;
}
