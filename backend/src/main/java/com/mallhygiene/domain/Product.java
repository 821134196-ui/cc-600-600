package com.mallhygiene.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** 正装商品批次：试用品领用时登记对应的正装批次 */
@Entity
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String brand;

    /** 正装批次号 */
    @Column(nullable = false, unique = true)
    public String batchNo;

    /** 生产日期 */
    public LocalDate productionDate;

    /** 未开封保质期（月） */
    @Column(nullable = false)
    public int shelfMonths;

    /** 开封后使用期限（月，PAO） */
    @Column(nullable = false)
    public int paoMonths;
}
